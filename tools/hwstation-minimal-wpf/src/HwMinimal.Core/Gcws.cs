using System.Globalization;
using System.Security.Cryptography;
using System.Text;

namespace HwMinimal;

public enum GcwsKind
{
    Read,
    SafetyStop,
    OperatorWrite,
    HeatStart,
    EngineeringWrite,
    Unsupported
}

public static class Gcws
{
    public const string HeatBlocked = "两路气压未同时达到 Android 资料中的正常条件，加热启动未发送。停止加热不受影响。";

    public static GcwsKind Classify(string? line)
    {
        if (string.IsNullOrWhiteSpace(line) || line.Contains('\n') || line.Contains('\r'))
        {
            return GcwsKind.Unsupported;
        }
        string upper = line.ToUpperInvariant();
        int first = upper.IndexOf("GCWS", StringComparison.Ordinal);
        if (first >= 0 && upper.IndexOf("GCWS", first + 4, StringComparison.Ordinal) >= 0)
        {
            return GcwsKind.Unsupported;
        }
        string verb = Verb(line);
        return verb switch
        {
            "HELLO" or "READ_OVEN_PROGRAM" or "READ_OVEN_PID" or "READ_INLET_PID" or "READ_DETECTOR_PID"
                or "READ_OVEN_TEMP" or "READ_INLET_TEMP" or "READ_DETECTOR_TEMP" or "READ_FID_STATUS"
                or "READ_GAS_PRESSURE" or "READ_CONTROL_STATUS" => GcwsKind.Read,
            "STOP_INLET_HEAT" or "STOP_DETECTOR_HEAT" or "STOP_OVEN_CONTROL" or "STOP_FID_VALVES" => GcwsKind.SafetyStop,
            "START_INLET_HEAT" or "START_DETECTOR_HEAT" or "START_OVEN_CONTROL" => GcwsKind.HeatStart,
            "WRITE_OVEN_PROGRAM" or "WRITE_INLET_TEMP" or "WRITE_DETECTOR_TEMP" or "WRITE_ZONE_SELECT"
                or "START_FID_IGNITE" or "START_FID_VALVES" or "FID_AUTO_ON" or "FID_AUTO_OFF" => GcwsKind.OperatorWrite,
            "WRITE_OVEN_PID" or "WRITE_INLET_PID" or "WRITE_DETECTOR_PID" => GcwsKind.EngineeringWrite,
            _ => GcwsKind.Unsupported
        };
    }

    public static string Verb(string line)
    {
        string trimmed = line.Trim();
        int tab = trimmed.IndexOf('\t');
        string token = tab >= 0 ? trimmed[(tab + 1)..] : trimmed;
        int cut = token.IndexOfAny(new[] { '\t', ' ' });
        if (cut >= 0)
        {
            token = token[..cut];
        }
        return token.ToUpperInvariant();
    }

    public static string Line(string verb) => "GCWS\t" + verb;

    public static string? Rejection(string verb, bool engineerOpen, string? payload)
    {
        if (payload != null && payload.Contains("GCWS", StringComparison.OrdinalIgnoreCase))
        {
            return "正文里夹有 GCWS，未发送。";
        }
        return Classify(Line(verb)) switch
        {
            GcwsKind.Unsupported => "未支持的命令，未发送。",
            GcwsKind.HeatStart => null,
            GcwsKind.EngineeringWrite when !engineerOpen => "工程师未授权，PID 未发送。没有默认口令。",
            _ => null
        };
    }
}

public static class GasSupply
{
    public const double NormalMpa = 0.1;
    public const long SampleMaxAgeMs = 3000;

    public static bool ChannelNormal(double? mpa)
    {
        return mpa.HasValue && double.IsFinite(mpa.Value) && mpa.Value >= NormalMpa;
    }

    public static string? BlockReason(double? hydrogen, double? air, long sampleAtMs, long nowMs)
    {
        if (sampleAtMs <= 0 || nowMs < sampleAtMs || nowMs - sampleAtMs > SampleMaxAgeMs)
        {
            return "气压读数缺失或已超过 3 秒，加热启动未发送。停止加热不受影响。";
        }
        if (!ChannelNormal(hydrogen) || !ChannelNormal(air))
        {
            return "氢气或空气低于 0.1 MPa，或不是有限值。这一门槛来自 2026-11-10 Android GasSupply，不是新造的。加热启动未发送。";
        }
        return null;
    }
}

public static class JsonFields
{
    public static bool Has(string json, string key) => json.Contains("\"" + key + "\"", StringComparison.Ordinal);

    public static bool TryNumber(string json, string key, out double value)
    {
        value = 0;
        int mark = json.IndexOf("\"" + key + "\"", StringComparison.Ordinal);
        if (mark < 0)
        {
            return false;
        }
        int colon = json.IndexOf(':', mark);
        if (colon < 0)
        {
            return false;
        }
        int i = colon + 1;
        while (i < json.Length && char.IsWhiteSpace(json[i]))
        {
            i++;
        }
        if (i >= json.Length)
        {
            return false;
        }
        bool quoted = json[i] == '"';
        if (quoted)
        {
            i++;
        }
        int start = i;
        while (i < json.Length && json[i] != '"' && json[i] != ',' && json[i] != '}' && json[i] != '\n' && json[i] != '\r')
        {
            i++;
        }
        string token = json[start..i].Trim();
        return double.TryParse(token, NumberStyles.Float, CultureInfo.InvariantCulture, out value) && double.IsFinite(value);
    }

    public static bool TryToken(string json, string key, out string token)
    {
        token = "";
        int mark = json.IndexOf("\"" + key + "\"", StringComparison.Ordinal);
        if (mark < 0)
        {
            return false;
        }
        int colon = json.IndexOf(':', mark);
        if (colon < 0)
        {
            return false;
        }
        int i = colon + 1;
        while (i < json.Length && char.IsWhiteSpace(json[i]))
        {
            i++;
        }
        if (i >= json.Length)
        {
            return false;
        }
        bool quoted = json[i] == '"';
        if (quoted)
        {
            i++;
        }
        int start = i;
        while (i < json.Length && json[i] != '"' && json[i] != ',' && json[i] != '}' && json[i] != '\n' && json[i] != '\r')
        {
            i++;
        }
        token = json[start..i].Trim();
        return token.Length > 0;
    }

    public static bool TryBool(string json, string key, out bool value)
    {
        value = false;
        if (!TryToken(json, key, out string token))
        {
            return false;
        }
        if (token.Equals("true", StringComparison.OrdinalIgnoreCase) || token == "1")
        {
            value = true;
            return true;
        }
        if (token.Equals("false", StringComparison.OrdinalIgnoreCase) || token == "0")
        {
            value = false;
            return true;
        }
        return false;
    }

    public static string Show(string json, string key)
    {
        return TryNumber(json, key, out double value) ? value.ToString(CultureInfo.InvariantCulture) : "未读取到";
    }
}

public sealed class EngineerGrant
{
    private byte[]? _salt;
    private byte[]? _hash;
    public bool IsOpen { get; private set; }

    public string Enroll(string? password)
    {
        if (string.IsNullOrEmpty(password) || password.Length < 8)
        {
            return "口令至少 8 位。未保存，没有默认口令。";
        }
        _salt = RandomNumberGenerator.GetBytes(16);
        _hash = Hash(password, _salt);
        IsOpen = false;
        return "已建立本进程口令。尚未解锁，未写入设备，也不读取 OpenChrom 的口令文件。";
    }

    public string Unlock(string? password)
    {
        if (_hash == null || _salt == null || password == null)
        {
            IsOpen = false;
            return "尚未建立口令，未解锁。";
        }
        byte[] got = Hash(password, _salt);
        if (!CryptographicOperations.FixedTimeEquals(got, _hash))
        {
            IsOpen = false;
            return "口令不对，未解锁。";
        }
        IsOpen = true;
        return "已解锁。只允许本进程随后的 PID 写入。";
    }

    public void Lock() => IsOpen = false;

    private static byte[] Hash(string password, byte[] salt)
    {
        return Rfc2898DeriveBytes.Pbkdf2(password, salt, 20_000, HashAlgorithmName.SHA256, 32);
    }
}

public sealed class StreamDemux
{
    private readonly FrameParser _frames = new();
    private readonly List<byte> _text = new();
    private bool _binary;
    private bool _json;
    private int _depth;
    private bool _string;
    private bool _escape;

    public bool Faulted { get; private set; }
    public string Fault { get; private set; } = "";

    public bool Push(byte[] data, int offset, int count, List<GcFrame> frames, List<string> texts)
    {
        if (Faulted)
        {
            return false;
        }
        for (int i = offset; i < offset + count; i++)
        {
            if (!Take(data[i], frames, texts))
            {
                return false;
            }
        }
        return true;
    }

    private bool Take(byte b, List<GcFrame> frames, List<string> texts)
    {
        if (_binary)
        {
            int before = frames.Count;
            if (!_frames.Push(new[] { b }, 0, 1, frames))
            {
                Faulted = true;
                Fault = _frames.Fault;
                _binary = false;
                return false;
            }
            if (frames.Count > before)
            {
                _binary = false;
            }
            return true;
        }
        if (_json)
        {
            return TakeJson(b, texts);
        }
        if (_text.Count > 0)
        {
            return TakeText(b, texts);
        }
        if (b is (byte)'\n' or (byte)'\r')
        {
            return true;
        }
        if (b == 0xAA)
        {
            _binary = true;
            return Take(b, frames, texts);
        }
        if (b == (byte)'{')
        {
            _json = true;
            _depth = 0;
            _string = false;
            _escape = false;
            _text.Clear();
            return TakeJson(b, texts);
        }
        _text.Clear();
        return TakeText(b, texts);
    }

    private bool TakeText(byte b, List<string> texts)
    {
        if (b == (byte)'\n')
        {
            texts.Add(Encoding.ASCII.GetString(_text.ToArray()).Trim('\r'));
            _text.Clear();
            return true;
        }
        _text.Add(b);
        if (_text.Count > 8192)
        {
            Faulted = true;
            Fault = "文本应答过长，已停止。";
            return false;
        }
        return true;
    }

    private bool TakeJson(byte b, List<string> texts)
    {
        _text.Add(b);
        if (_text.Count > 8192)
        {
            Faulted = true;
            Fault = "JSON 应答过长，已停止。";
            return false;
        }
        if (_escape)
        {
            _escape = false;
            return true;
        }
        if (_string)
        {
            if (b == (byte)'\\')
            {
                _escape = true;
            }
            else if (b == (byte)'"')
            {
                _string = false;
            }
            return true;
        }
        if (b == (byte)'"')
        {
            _string = true;
        }
        else if (b == (byte)'{')
        {
            _depth++;
        }
        else if (b == (byte)'}')
        {
            _depth--;
            if (_depth == 0)
            {
                texts.Add(Encoding.UTF8.GetString(_text.ToArray()));
                _text.Clear();
                _json = false;
            }
        }
        return true;
    }
}
