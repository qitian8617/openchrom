using System.Text;

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
    public int FirstPointWaitMs { get; init; } = 3000;
    public int StopDrainMs { get; init; } = 1200;
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
    private int _analysisStart;
    private int _analysisStop;
    private int _analysisAccepted;
    private HwFrequenceParse _analysisConfig;
    private double? _h2;
    private double? _air;
    private long _gasAtMs;
    private readonly SemaphoreSlim _write = new(1, 1);
    private readonly object _exchangeGate = new();
    private TaskCompletionSource<string>? _exchange;
    public EngineerGrant Engineer { get; } = new();
    public string AnalysisPhase { get; private set; } = "Idle";

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
    public void RequestStartAnalysis(HwFrequenceParse config, bool acceptedLimits)
    {
        _analysisConfig = config;
        Volatile.Write(ref _analysisAccepted, acceptedLimits ? 1 : 0);
        Interlocked.Exchange(ref _analysisStart, 1);
    }
    public void RequestStopAnalysis() => Interlocked.Exchange(ref _analysisStop, 1);

    public void NoteGas(double? hydrogenMpa, double? airMpa, long sampleAtMs)
    {
        _h2 = hydrogenMpa;
        _air = airMpa;
        _gasAtMs = sampleAtMs;
    }

    public async Task<string> ExchangeAsync(string verb, string? payload, CancellationToken cancellationToken)
    {
        if (Gcws.Classify(Gcws.Line(verb)) == GcwsKind.HeatStart)
        {
            string? gas = GasSupply.BlockReason(_h2, _air, _gasAtMs, Environment.TickCount64);
            if (gas != null)
            {
                return gas;
            }
        }
        string? rejected = Gcws.Rejection(verb, Engineer.IsOpen, payload);
        if (rejected != null)
        {
            return rejected;
        }
        var waiter = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);
        lock (_exchangeGate)
        {
            if (_exchange != null)
            {
                return "上一条命令尚未结束，未发送。";
            }
            _exchange = waiter;
        }
        try
        {
            await _write.WaitAsync(cancellationToken).ConfigureAwait(false);
            try
            {
                byte[] line = Encoding.ASCII.GetBytes(Gcws.Line(verb) + "\r\n");
                await _transport.WriteAsync(line, cancellationToken).ConfigureAwait(false);
                if (payload != null)
                {
                    await _transport.WriteAsync(Encoding.UTF8.GetBytes(payload + "\n"), cancellationToken).ConfigureAwait(false);
                }
            }
            finally
            {
                _write.Release();
            }
            using var limit = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
            limit.CancelAfter(TimeSpan.FromSeconds(3));
            return await waiter.Task.WaitAsync(limit.Token).ConfigureAwait(false);
        }
        catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested)
        {
            return "命令超时，没有应答。未把超时当成成功。";
        }
        finally
        {
            lock (_exchangeGate)
            {
                if (ReferenceEquals(_exchange, waiter))
                {
                    _exchange = null;
                }
            }
        }
    }

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
        reason = "转发已允许。每个样本先 data3=3，紧接 data3=4 且 B=0。返回值不是采集成功。";
        return true;
    }

    public async Task RunAsync(CancellationToken cancellationToken)
    {
        var hello = new HelloReader();
        var demux = new StreamDemux();
        var run = new AcqRun();
        var frames = new List<GcFrame>();
        var texts = new List<string>();
        string phase = "Idle";
        bool hwTake = false;
        long pairDeadline = 0;
        long drainDeadline = 0;
        int drainPairs = 0;
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
                    await DriveAnalysisAsync(() => pcSeq++, () => acquiring, v => acquiring = v, v => waitStart = v, v => startDeadline = v, () => phase, v => phase = v, () => hwTake, v => hwTake = v, v => pairDeadline = v, v => drainDeadline = v, () => drainPairs, v => drainPairs = v, cancellationToken).ConfigureAwait(false);
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
                texts.Clear();
                bool parsed = demux.Push(buffer, binaryStart, n - binaryStart, frames, texts);
                foreach (string text in texts)
                {
                    lock (_exchangeGate)
                    {
                        _exchange?.TrySetResult(text);
                    }
                }
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
                            if (phase == "WaitAck")
                            {
                                phase = "WaitPair";
                                _pairDeadlineSeen = Environment.TickCount64 + _options.FirstPointWaitMs;
                                pairDeadline = _pairDeadlineSeen;
                                Interlocked.Exchange(ref _samplesArmed, 1);
                                Status?.Invoke("设备已确认 START_ACQ。尚未通知 HW 取数，要等第一个真实点。");
                            }
                            else
                            {
                                Status?.Invoke("设备已确认 START_ACQ。这只说明设备开始送数，不是工作站采集成功。");
                            }
                        }
                        else if (phase == "WaitAck")
                        {
                            phase = "Idle";
                            AnalysisPhase = phase;
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
                        if (phase == "Draining")
                        {
                            Status?.Invoke(frame.Command == GcCodec.CmdAck
                                ? "设备已确认 STOP_ACQ。仍接收有限的真实点，然后停止 HW。这不是停热。"
                                : "STOP_ACQ 被拒绝。仍按有限时间收口。这不是停热。");
                            continue;
                        }
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
                                if (sent >= emitted.Count || !SendSamplePair(emitted[sent]))
                                {
                                    Sample?.Invoke(sample);
                                    if (phase is "WaitPair" or "Running" or "Draining")
                                    {
                                        await FailAnalysisAsync(() => pcSeq++, hwTake, "样本转发失败").ConfigureAwait(false);
                                    hwTake = false;
                                        phase = "Invalid";
                                    }
                                    break;
                                }
                                sent++;
                                if (phase == "WaitPair")
                                {
                                    HwCallResult taken = _sink.Send(1, 0, 1);
                                    Status?.Invoke(taken.Text + " HW 记录未确认。");
                                    if (taken.Code != 1)
                                    {
                                        await FailAnalysisAsync(() => pcSeq++, false, "HW 开始取数没有发出，本次无效。").ConfigureAwait(false);
                                        phase = "Invalid";
                                    }
                                    else
                                    {
                                        hwTake = true;
                                        phase = "Running";
                                    }
                                }
                                else if (phase == "Draining")
                                {
                                    drainPairs++;
                                }
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
                    run.Invalidate(demux.Fault);
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

    private async Task DriveAnalysisAsync(
        Func<byte> nextSeq,
        Func<bool> acquiring,
        Action<bool> setAcquiring,
        Action<bool> setWaitStart,
        Action<long> setStartDeadline,
        Func<string> phase,
        Action<string> setPhase,
        Func<bool> hwTake,
        Action<bool> setHwTake,
        Action<long> setPairDeadline,
        Action<long> setDrainDeadline,
        Func<int> drainPairs,
        Action<int> setDrainPairs,
        CancellationToken cancellationToken)
    {
        long now = Environment.TickCount64;
        AnalysisPhase = phase();
        if (phase() == "WaitPair" && now > _pairDeadlineSeen)
        {
            Status?.Invoke("没有等到真实点，未通知 HW 取数。已请求停止设备采集。");
            await LockedWrite(GcCodec.StopAcq(nextSeq()), cancellationToken).ConfigureAwait(false);
            DisarmSamples();
            setAcquiring(false);
            setPhase("Idle");
            AnalysisPhase = "Idle";
            return;
        }
        if (phase() == "Draining" && (now > _drainDeadline || drainPairs() >= 4))
        {
            if (hwTake())
            {
                HwCallResult stop = _sink.Send(1, 0, 2);
                setHwTake(false);
                Status?.Invoke(stop.Text + " 停止分析已请求 HW 停止取数。HW 记录未确认。这不是停热。");
            }
            else
            {
                Status?.Invoke("停止分析结束。没有通知过 HW 取数。这不是停热。");
            }
            DisarmSamples();
            setAcquiring(false);
            setPhase("Idle");
            AnalysisPhase = "Idle";
            return;
        }
        if (Interlocked.Exchange(ref _analysisStart, 0) == 1 && phase() == "Idle")
        {
            if (!ForwardGate.Allows(_analysisConfig, Volatile.Read(ref _analysisAccepted) == 1, out string reason))
            {
                Status?.Invoke(reason + " 未开始分析。");
                return;
            }
            if (!_sink.IsReady)
            {
                Status?.Invoke("DLL 尚未成功加载，未开始分析。");
                return;
            }
            await LockedWrite(GcCodec.StartAcq(nextSeq(), AcqRun.IntervalMs), cancellationToken).ConfigureAwait(false);
            setWaitStart(true);
            setStartDeadline(now + _options.AckTimeoutMs);
            setPhase("WaitAck");
            AnalysisPhase = "WaitAck";
            Status?.Invoke("开始分析：已请求设备采集，等待确认。尚未通知 HW。");
        }
        if (Interlocked.Exchange(ref _analysisStop, 0) == 1)
        {
            if (phase() is "WaitPair" or "Running" or "Draining")
            {
                await LockedWrite(GcCodec.StopAcq(nextSeq()), cancellationToken).ConfigureAwait(false);
                setPhase("Draining");
                _drainDeadline = now + _options.StopDrainMs;
                setDrainDeadline(_drainDeadline);
                setDrainPairs(0);
                AnalysisPhase = "Draining";
                Status?.Invoke("停止分析：已请求停止设备采集。随后有限真实点仍转发，然后停止 HW。这不是停热。");
            }
            else if (phase() == "WaitAck")
            {
                setWaitStart(false);
                setPhase("Idle");
                AnalysisPhase = "Idle";
                Status?.Invoke("开始尚未确认，未通知 HW。");
            }
        }
    }

    private long _drainDeadline;
    private long _pairDeadlineSeen;

    private async Task FailAnalysisAsync(Func<byte> nextSeq, bool hwTake, string why)
    {
        DisarmSamples();
        AnalysisPhase = "Invalid";
        await LockedWrite(GcCodec.StopAcq(nextSeq()), CancellationToken.None).ConfigureAwait(false);
        if (hwTake)
        {
            HwCallResult stop = _sink.Send(1, 0, 2);
            Status?.Invoke(why + " " + stop.Text + " 本次无效。");
        }
        else
        {
            Status?.Invoke(why + " 已请求停止设备采集。本次无效。");
        }
    }

    private async Task LockedWrite(byte[] data, CancellationToken cancellationToken)
    {
        await _write.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            await _transport.WriteAsync(data, cancellationToken).ConfigureAwait(false);
        }
        finally
        {
            _write.Release();
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

    private bool SendSamplePair(HwTriple channelA)
    {
        if (channelA.Data3 != 3)
        {
            DisarmSamples();
            Status?.Invoke("样本不是 A 通道命令，已停止转发。未补 B。");
            return false;
        }
        if (!Send(channelA))
        {
            return false;
        }
        return Send(new HwTriple(0, 0, 4));
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
