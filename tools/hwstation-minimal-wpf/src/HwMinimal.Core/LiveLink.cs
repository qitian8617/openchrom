namespace HwMinimal;

public interface IByteTransport
{
    ValueTask WriteAsync(ReadOnlyMemory<byte> data, CancellationToken cancellationToken);
    ValueTask<int> ReadAsync(Memory<byte> buffer, CancellationToken cancellationToken);
}

public readonly record struct HwCallResult(int Data1, int Data2, int Data3, int Code, string Text);

public interface IHwSink
{
    bool IsReady { get; }
    HwCallResult Send(int data1, int data2, int data3);
}

public sealed class LinkOptions
{
    public int HeartbeatMs { get; init; } = 1000;
    public int AckTimeoutMs { get; init; } = 2000;
}

public sealed class LiveLink
{
    private readonly IByteTransport _transport;
    private readonly IHwSink _sink;
    private readonly LinkOptions _options;
    private int _start;
    private int _stop;
    private int _hwStart;
    private int _hwStop;
    private int _hwZero;
    private int _samplesArmed;
    private bool _faultAnnounced;

    public LiveLink(IByteTransport transport, IHwSink sink, LinkOptions? options = null)
    {
        _transport = transport;
        _sink = sink;
        _options = options ?? new LinkOptions();
    }

    public event Action<string>? Status;
    public event Action<ObservedSample>? Sample;
    public bool SamplesArmed => Volatile.Read(ref _samplesArmed) == 1;

    public void RequestDeviceStart() => Interlocked.Exchange(ref _start, 1);
    public void RequestDeviceStop() => Interlocked.Exchange(ref _stop, 1);
    public void RequestHwRecordStart() => Interlocked.Exchange(ref _hwStart, 1);
    public void RequestHwRecordStop() => Interlocked.Exchange(ref _hwStop, 1);
    public void RequestHwZeroA() => Interlocked.Exchange(ref _hwZero, 1);

    public void DisarmSamples() => Interlocked.Exchange(ref _samplesArmed, 0);

    public bool TryArm(HwFrequenceParse parse, bool acceptedLimits, out string reason)
    {
        if (!ForwardGate.Allows(parse, acceptedLimits, out reason))
        {
            DisarmSamples();
            return false;
        }
        if (!_sink.IsReady)
        {
            reason = "DLL 尚未成功加载，未转发。";
            DisarmSamples();
            return false;
        }
        Interlocked.Exchange(ref _samplesArmed, 1);
        reason = "转发已允许。样本使用 data3=3，data2=0。返回值不是采集成功。";
        return true;
    }

    public async Task RunAsync(CancellationToken cancellationToken)
    {
        var hello = new HelloReader();
        var parser = new FrameParser();
        var run = new AcqRun();
        var frames = new List<GcFrame>();
        byte[] buffer = new byte[2048];
        byte pcSeq = 1;
        bool greeted = false;
        bool acquiring = false;
        bool waitStart = false;
        bool waitStop = false;
        long startDeadline = 0;
        long stopDeadline = 0;
        long lastBeat = 0;
        bool notedPanel = false;
        try
        {
            await _transport.WriteAsync(GcCodec.HelloPc, cancellationToken).ConfigureAwait(false);
            while (!cancellationToken.IsCancellationRequested)
            {
                if (greeted)
                {
                    await DrainAsync(run, () => pcSeq++, () => acquiring, v => acquiring = v, () => waitStart, v => waitStart = v, () => waitStop, v => waitStop = v, v => startDeadline = v, v => stopDeadline = v, cancellationToken).ConfigureAwait(false);
                    long nowBeat = Environment.TickCount64;
                    if (lastBeat == 0 || nowBeat - lastBeat >= _options.HeartbeatMs)
                    {
                        await WriteCmd(GcCodec.CmdHeartbeat, pcSeq++, Array.Empty<byte>(), cancellationToken).ConfigureAwait(false);
                        lastBeat = nowBeat;
                    }
                }
                int n = await _transport.ReadAsync(buffer, cancellationToken).ConfigureAwait(false);
                long now = Environment.TickCount64;
                if (waitStart && now > startDeadline)
                {
                    waitStart = false;
                    acquiring = false;
                    Status?.Invoke("START_ACQ 未确认。采集没有开始。");
                }
                if (waitStop && now > stopDeadline)
                {
                    waitStop = false;
                    acquiring = false;
                    run.Reset();
                    Status?.Invoke("STOP_ACQ 未确认。这不是急停，也没有关闭温控。");
                }
                if (n < 0)
                {
                    Status?.Invoke("连接已关闭。关闭本身不会发送加热或点火命令。");
                    break;
                }
                if (n == 0)
                {
                    continue;
                }
                int binaryStart = 0;
                if (!greeted)
                {
                    int used = hello.Push(buffer, 0, n);
                    if (!hello.Done)
                    {
                        continue;
                    }
                    if (hello.Error != null)
                    {
                        Status?.Invoke(hello.Error);
                        break;
                    }
                    greeted = true;
                    lastBeat = Environment.TickCount64;
                    Status?.Invoke("HELLO_OK。采集尚未开始。DLL 返回值不会被当成采集成功。");
                    binaryStart = used;
                }
                if (binaryStart >= n)
                {
                    continue;
                }
                frames.Clear();
                bool parsed = parser.Push(buffer, binaryStart, n - binaryStart, frames);
                foreach (GcFrame frame in frames)
                {
                    if (waitStart && (frame.Command is GcCodec.CmdAck or GcCodec.CmdNack) && frame.Payload.Length >= 2 && frame.Payload[0] == GcCodec.CmdStart)
                    {
                        waitStart = false;
                        if (frame.Command == GcCodec.CmdAck && frame.Payload[1] == 0)
                        {
                            acquiring = true;
                            run.Reset();
                            _faultAnnounced = false;
                            Status?.Invoke("设备已确认 START_ACQ。这只说明设备开始送数，不是工作站采集成功。");
                        }
                        else
                        {
                            acquiring = false;
                            Status?.Invoke("START_ACQ 被拒绝或应答无效。采集没有开始。");
                        }
                        continue;
                    }
                    if (waitStop && (frame.Command is GcCodec.CmdAck or GcCodec.CmdNack) && frame.Payload.Length >= 1 && frame.Payload[0] == GcCodec.CmdStop)
                    {
                        waitStop = false;
                        acquiring = false;
                        run.Reset();
                        Status?.Invoke(frame.Command == GcCodec.CmdAck
                            ? "设备已确认 STOP_ACQ。这不是急停，也不会关闭温控或气路。"
                            : "STOP_ACQ 被拒绝。这不是急停，也不会关闭温控。");
                        continue;
                    }
                    if (frame.Command == GcCodec.CmdData)
                    {
                        if (!acquiring)
                        {
                            Status?.Invoke("收到 ACQ_DATA，但 START_ACQ 尚未确认。未转发。");
                            continue;
                        }
                        var view = new List<ObservedSample>();
                        var emitted = new List<HwTriple>();
                        bool wasInvalid = run.Invalid;
                        bool armed = Volatile.Read(ref _samplesArmed) == 1;
                        run.Accept(frame, armed && !wasInvalid, view, emitted);
                        int sent = 0;
                        foreach (ObservedSample sample in view)
                        {
                            if (sample.Forwarded)
                            {
                                if (sent >= emitted.Count || !Send(emitted[sent]))
                                {
                                    Sample?.Invoke(sample);
                                    break;
                                }
                                sent++;
                            }
                            Sample?.Invoke(sample);
                        }
                        if (run.Invalid && !wasInvalid)
                        {
                            MarkInvalid(run.Fault);
                        }
                        continue;
                    }
                    if (frame.Command == GcCodec.CmdError)
                    {
                        run.Invalidate("设备返回错误帧。已停止转发。");
                        MarkInvalid(run.Fault);
                        continue;
                    }
                    if (frame.Command == GcCodec.CmdDone)
                    {
                        acquiring = false;
                        Status?.Invoke("收到 ACQ_DONE。之后的数据不再当作本次采集。");
                        continue;
                    }
                    if (frame.Command == GcCodec.CmdPanel && !notedPanel)
                    {
                        notedPanel = true;
                        Status?.Invoke("收到面板通知，未当作样本。");
                        continue;
                    }
                    if (frame.Command is GcCodec.CmdAck or GcCodec.CmdNack or GcCodec.CmdHeartbeat or GcCodec.CmdPanel)
                    {
                        continue;
                    }
                    run.Invalidate($"收到未支持的命令 0x{frame.Command:X2}。已停止转发。");
                    MarkInvalid(run.Fault);
                }
                if (!parsed)
                {
                    run.Invalidate(parser.Fault);
                    MarkInvalid(run.Fault);
                }
            }
        }
        catch (OperationCanceledException)
        {
            Status?.Invoke("本地已取消。没有发送加热或点火命令。");
        }
        catch (Exception ex)
        {
            Status?.Invoke("连接中断：" + ex.Message);
        }
    }

    private async Task DrainAsync(
        AcqRun run,
        Func<byte> nextSeq,
        Func<bool> acquiring,
        Action<bool> setAcquiring,
        Func<bool> waitStart,
        Action<bool> setWaitStart,
        Func<bool> waitStop,
        Action<bool> setWaitStop,
        Action<long> setStartDeadline,
        Action<long> setStopDeadline,
        CancellationToken cancellationToken)
    {
        if (Interlocked.Exchange(ref _start, 0) == 1 && !acquiring() && !waitStart())
        {
            await _transport.WriteAsync(GcCodec.StartAcq(nextSeq(), AcqRun.IntervalMs), cancellationToken).ConfigureAwait(false);
            setWaitStart(true);
            setStartDeadline(Environment.TickCount64 + _options.AckTimeoutMs);
            Status?.Invoke("已发送二进制 START_ACQ 100 ms，等待设备确认。");
        }
        if (Interlocked.Exchange(ref _stop, 0) == 1)
        {
            await _transport.WriteAsync(GcCodec.StopAcq(nextSeq()), cancellationToken).ConfigureAwait(false);
            setWaitStop(true);
            setWaitStart(false);
            setAcquiring(false);
            run.Reset();
            _faultAnnounced = false;
            setStopDeadline(Environment.TickCount64 + _options.AckTimeoutMs);
            Status?.Invoke("已发送二进制 STOP_ACQ。这不是急停，也不会关闭温控或气路。");
        }
        if (!_sink.IsReady)
        {
            if (Interlocked.Exchange(ref _hwStart, 0) == 1
                || Interlocked.Exchange(ref _hwStop, 0) == 1
                || Interlocked.Exchange(ref _hwZero, 0) == 1)
            {
                Status?.Invoke("DLL 未就绪，工作站命令未发送。");
            }
            return;
        }
        if (Interlocked.Exchange(ref _hwStart, 0) == 1)
        {
            Send(new HwTriple(1, 0, 1));
        }
        if (Interlocked.Exchange(ref _hwStop, 0) == 1)
        {
            Send(new HwTriple(1, 0, 2));
        }
        if (Interlocked.Exchange(ref _hwZero, 0) == 1)
        {
            Send(new HwTriple(1, 0, 5));
        }
    }

    private async Task WriteCmd(byte command, byte sequence, byte[] payload, CancellationToken cancellationToken)
    {
        byte[] frame = GcCodec.Encode(command, sequence, payload);
        await _transport.WriteAsync(frame, cancellationToken).ConfigureAwait(false);
    }

    private void MarkInvalid(string reason)
    {
        DisarmSamples();
        if (_faultAnnounced)
        {
            return;
        }
        _faultAnnounced = true;
        Status?.Invoke(reason + " 请先停止本次采集。" + LogCurrent.Caveat);
    }

    private bool Send(HwTriple triple)
    {
        try
        {
            HwCallResult result = _sink.Send(triple.Data1, triple.Data2, triple.Data3);
            Status?.Invoke($"DLL {triple.Data1},{triple.Data2},{triple.Data3} → {result.Text}");
            if (result.Code < 0)
            {
                DisarmSamples();
                return false;
            }
            return true;
        }
        catch (Exception ex)
        {
            DisarmSamples();
            Status?.Invoke("DLL 调用失败：" + ex.Message + "。已停止转发。");
            return false;
        }
    }
}
