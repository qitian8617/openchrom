using System.Buffers.Binary;
using System.Text;

namespace HwMinimal;

public static class Crc16
{
    public static ushort Compute(ReadOnlySpan<byte> data)
    {
        ushort crc = 0xFFFF;
        foreach (byte b in data)
        {
            crc ^= b;
            for (int i = 0; i < 8; i++)
            {
                crc = (ushort)(((crc & 1) != 0) ? ((crc >> 1) ^ 0xA001) : (crc >> 1));
            }
        }
        return crc;
    }
}

public readonly record struct GcFrame(byte Command, byte Sequence, byte[] Payload);

public static class GcCodec
{
    public const byte Hdr0 = 0xAA;
    public const byte Hdr1 = 0x55;
    public const byte Tail0 = 0x55;
    public const byte Tail1 = 0xAA;
    public const int MaxPayload = 128;
    public const byte CmdStart = 0x01;
    public const byte CmdStop = 0x02;
    public const byte CmdHeartbeat = 0x03;
    public const byte CmdData = 0x10;
    public const byte CmdDone = 0x11;
    public const byte CmdError = 0x7F;
    public const byte CmdPanel = 0x21;
    public const byte CmdAck = 0x80;
    public const byte CmdNack = 0x81;

    public static readonly byte[] HelloPc = Encoding.ASCII.GetBytes("GCWS\tHELLO\tPC\r\n");

    public static byte[] Encode(byte command, byte sequence, ReadOnlySpan<byte> payload)
    {
        if (payload.Length > MaxPayload)
        {
            throw new ArgumentOutOfRangeException(nameof(payload));
        }
        byte[] frame = new byte[10 + payload.Length];
        frame[0] = Hdr0;
        frame[1] = Hdr1;
        frame[2] = command;
        frame[3] = sequence;
        frame[4] = (byte)payload.Length;
        frame[5] = (byte)(payload.Length >> 8);
        payload.CopyTo(frame.AsSpan(6));
        ushort crc = Crc16.Compute(frame.AsSpan(2, 4 + payload.Length));
        frame[6 + payload.Length] = (byte)crc;
        frame[7 + payload.Length] = (byte)(crc >> 8);
        frame[8 + payload.Length] = Tail0;
        frame[9 + payload.Length] = Tail1;
        return frame;
    }

    public static byte[] StartAcq(byte sequence, ushort intervalMs)
    {
        byte[] payload = new byte[2];
        BinaryPrimitives.WriteUInt16LittleEndian(payload, intervalMs);
        return Encode(CmdStart, sequence, payload);
    }

    public static byte[] StopAcq(byte sequence) => Encode(CmdStop, sequence, ReadOnlySpan<byte>.Empty);
}

public sealed class FrameParser
{
    private readonly byte[] _buf = new byte[10 + GcCodec.MaxPayload];
    private int _len;

    public bool Faulted { get; private set; }
    public string Fault { get; private set; } = "";

    public bool Push(byte[] data, int offset, int count, List<GcFrame> into)
    {
        return Push(data.AsSpan(offset, count), into);
    }

    public bool Push(ReadOnlySpan<byte> data, List<GcFrame> into)
    {
        if (Faulted)
        {
            return false;
        }
        foreach (byte b in data)
        {
            if (!PushOne(b, into))
            {
                return false;
            }
        }
        return true;
    }

    private bool Fail(string reason)
    {
        Faulted = true;
        Fault = reason;
        _len = 0;
        return false;
    }

    private bool PushOne(byte b, List<GcFrame> into)
    {
        if (_len == 0 && b != GcCodec.Hdr0)
        {
            return Fail("二进制流出现非 AA 字节，已停止。不跳过、不重同步。");
        }
        if (_len >= _buf.Length)
        {
            return Fail("帧长超出上限，已停止。");
        }
        _buf[_len++] = b;
        if (_len == 2 && _buf[1] != GcCodec.Hdr1)
        {
            return Fail("帧头不是 AA 55，已停止。不跳过、不重同步。");
        }
        if (_len < 6)
        {
            return true;
        }
        int plen = _buf[4] | (_buf[5] << 8);
        if (plen > GcCodec.MaxPayload)
        {
            return Fail("载荷长度超过 128，已停止。");
        }
        int need = 10 + plen;
        if (_len < need)
        {
            return true;
        }
        if (_buf[8 + plen] != GcCodec.Tail0 || _buf[9 + plen] != GcCodec.Tail1)
        {
            return Fail("帧尾不是 55 AA，已停止。不跳过、不重同步。");
        }
        ushort rx = (ushort)(_buf[6 + plen] | (_buf[7 + plen] << 8));
        ushort calc = Crc16.Compute(_buf.AsSpan(2, 4 + plen));
        if (rx != calc)
        {
            return Fail("CRC 不一致，已停止。不跳过坏帧。");
        }
        byte[] payload = plen == 0 ? Array.Empty<byte>() : _buf.AsSpan(6, plen).ToArray();
        into.Add(new GcFrame(_buf[2], _buf[3], payload));
        _len = 0;
        return true;
    }
}

public sealed class HelloReader
{
    private readonly List<byte> _line = new();

    public bool Done { get; private set; }
    public string? Error { get; private set; }

    public int Push(byte[] data, int offset, int count)
    {
        return Push(data.AsSpan(offset, count));
    }

    public int Push(ReadOnlySpan<byte> chunk)
    {
        if (Done)
        {
            return 0;
        }
        for (int i = 0; i < chunk.Length; i++)
        {
            byte b = chunk[i];
            if (b == (byte)'\n')
            {
                string line = Encoding.ASCII.GetString(_line.ToArray()).Trim('\r', ' ', '\t');
                Done = true;
                if (line != "HELLO_OK")
                {
                    Error = "握手应答不是 HELLO_OK。";
                }
                return i + 1;
            }
            _line.Add(b);
            if (_line.Count > 64)
            {
                Done = true;
                Error = "握手应答过长。";
                return i + 1;
            }
        }
        return chunk.Length;
    }
}
