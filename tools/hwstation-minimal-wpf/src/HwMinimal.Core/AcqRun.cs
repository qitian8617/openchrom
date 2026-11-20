using System.Buffers.Binary;

namespace HwMinimal;

public readonly record struct ObservedSample(int TimeMs, float MilliVolts, int FemtoAmps, bool Forwarded, string Note);

public sealed class AcqRun
{
    public const int IntervalMs = 100;

    private bool _haveTime;
    private int _lastTime;
    private bool _haveSeq;
    private int _lastSeq;
    private bool _haveBatch;
    private int _lastBatch;

    public bool Invalid { get; private set; }
    public string Fault { get; private set; } = "";

    public void Reset()
    {
        _haveTime = false;
        _lastTime = 0;
        _haveSeq = false;
        _lastSeq = 0;
        _haveBatch = false;
        _lastBatch = 0;
        Invalid = false;
        Fault = "";
    }

    public void Invalidate(string reason)
    {
        if (!Invalid)
        {
            Invalid = true;
            Fault = reason;
        }
    }

    public void Accept(GcFrame frame, bool forward, IList<ObservedSample> view, IList<HwTriple> sink)
    {
        if (Invalid || frame.Command != GcCodec.CmdData)
        {
            return;
        }
        if (!CheckSeq(frame.Sequence) || !TryReadBatch(frame.Payload, out int batch, out int count))
        {
            return;
        }
        for (int i = 0; i < count; i++)
        {
            int offset = 6 + i * 8;
            int time = BinaryPrimitives.ReadInt32LittleEndian(frame.Payload.AsSpan(offset));
            float y = BitConverter.Int32BitsToSingle(BinaryPrimitives.ReadInt32LittleEndian(frame.Payload.AsSpan(offset + 4)));
            if (!AcceptTime(time))
            {
                return;
            }
            MapResult map = LogCurrent.MapMilliVolts(y);
            if (!map.Ok)
            {
                Fail(map.Error + " 该点未送出。");
                return;
            }
            view.Add(new ObservedSample(time, y, map.FemtoAmps, forward, ""));
            if (forward)
            {
                sink.Add(new HwTriple(map.FemtoAmps, 0, 3));
            }
        }
        _haveBatch = true;
        _lastBatch = batch;
    }

    private bool CheckSeq(int seq)
    {
        if (_haveSeq && seq != ((_lastSeq + 1) & 0xFF))
        {
            return Fail("设备帧序号不连续，本次作废。未跳过缺口，未补点。");
        }
        _haveSeq = true;
        _lastSeq = seq;
        return true;
    }

    private bool TryReadBatch(byte[] payload, out int batch, out int count)
    {
        batch = 0;
        count = 0;
        if (payload.Length < 6)
        {
            return Fail("ACQ_DATA 过短，本次作废。");
        }
        batch = BinaryPrimitives.ReadInt32LittleEndian(payload);
        count = BinaryPrimitives.ReadUInt16LittleEndian(payload.AsSpan(4));
        if (count <= 0 || payload.Length != 6 + count * 8)
        {
            return Fail("ACQ_DATA 点数与长度不符，本次作废。");
        }
        if (!_haveBatch && batch != 0)
        {
            return Fail("首批序号不是 0，前面可能有缺口。本次作废，未补点。");
        }
        if (_haveBatch && batch != _lastBatch + 1)
        {
            return Fail("批次序号不连续，本次作废。未跳过缺口，未补点。");
        }
        return true;
    }

    private bool AcceptTime(int time)
    {
        if (!_haveTime)
        {
            if (time != 0)
            {
                return Fail("首点时间不是 0 ms，本次作废。未把中间时间补 0。");
            }
        }
        else if (time != _lastTime + IntervalMs)
        {
            return Fail($"时间间隔是 {time - _lastTime} ms，不是 {IntervalMs} ms。本次作废。未补点，未压缩时间。相同数值也不当作过期。");
        }
        _haveTime = true;
        _lastTime = time;
        return true;
    }

    private bool Fail(string reason)
    {
        Invalid = true;
        Fault = reason;
        return false;
    }
}
