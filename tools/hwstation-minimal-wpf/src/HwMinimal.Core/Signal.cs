using System.Globalization;
using System.Text.RegularExpressions;

namespace HwMinimal;

public readonly record struct HwTriple(int Data1, int Data2, int Data3);

public readonly record struct MapResult(bool Ok, int FemtoAmps, string Error)
{
    public static MapResult Good(int fa) => new(true, fa, "");
    public static MapResult Bad(string error) => new(false, 0, error);
}

public static class LogCurrent
{
    public const string Caveat = "无法验证设备样本新鲜度，仅接入验证，不用于正式定量";

    public static MapResult MapMilliVolts(float y)
    {
        if (float.IsNaN(y) || float.IsInfinity(y))
        {
            return MapResult.Bad("Y 不是有限毫伏值，已停止转发，未截断。");
        }
        double decades = y / 200.0;
        double fa = FemtoAmps(decades);
        if (!double.IsFinite(fa))
        {
            return MapResult.Bad("换算结果不是有限值，已停止转发，未截断。");
        }
        double rounded = Math.Round(fa, MidpointRounding.AwayFromZero);
        if (rounded > int.MaxValue || rounded < int.MinValue)
        {
            return MapResult.Bad("换算超出 int32，已停止转发，未截断。");
        }
        return MapResult.Good((int)rounded);
    }

    private static double FemtoAmps(double decades)
    {
        double nearest = Math.Round(decades);
        if (Math.Abs(decades - nearest) <= 1e-9 && nearest is >= -9 and <= 9)
        {
            double value = 100_000_000d;
            int n = (int)nearest;
            if (n >= 0)
            {
                for (int i = 0; i < n; i++)
                {
                    value *= 10d;
                }
            }
            else
            {
                for (int i = 0; i < -n; i++)
                {
                    value /= 10d;
                }
            }
            return value;
        }
        return 100_000_000d * Math.Pow(10.0, decades);
    }
}

public readonly record struct HwFrequenceParse(
    bool SyntaxOk,
    int DesiredHz,
    int NativeHz,
    bool FileRuleOk,
    bool CompatibleWith100Ms,
    string Detail);

public static partial class HwFrequence
{
    [GeneratedRegex(@"^\s*(\d+)\s*,\s*(\d+)\s*$")]
    private static partial Regex Pair();

    public static HwFrequenceParse Parse(string? text)
    {
        if (string.IsNullOrWhiteSpace(text))
        {
            return new HwFrequenceParse(false, 0, 0, false, false, "HWFrequence 是空的，不能转发。");
        }
        string? pair = null;
        foreach (string raw in text.Replace("\r\n", "\n").Split('\n'))
        {
            string line = raw.Trim();
            if (line.Length == 0 || line.StartsWith('#') || line.StartsWith("注", StringComparison.Ordinal))
            {
                continue;
            }
            pair = line;
            break;
        }
        if (pair == null)
        {
            return new HwFrequenceParse(false, 0, 0, false, false, "HWFrequence 没有频率行，不能转发。");
        }
        Match match = Pair().Match(pair);
        if (!match.Success
            || !int.TryParse(match.Groups[1].Value, NumberStyles.None, CultureInfo.InvariantCulture, out int desired)
            || !int.TryParse(match.Groups[2].Value, NumberStyles.None, CultureInfo.InvariantCulture, out int native))
        {
            return new HwFrequenceParse(false, 0, 0, false, false, "HWFrequence 第一行不是“期望Hz,固有Hz”。文件未修改。");
        }
        bool rule = desired >= 1 && native >= 1 && desired <= native && native % desired == 0;
        bool ms100 = rule && desired == 10;
        string detail = !rule
            ? $"频率 {desired},{native} 不符合“前者不大于后者且能被除尽”。不能转发。文件未修改。"
            : ms100
                ? $"频率 {desired},{native} 的期望周期是 100 ms，与本次 START_ACQ 请求一致。文件未修改，也没有部署。"
                : $"频率 {desired},{native} 的期望周期是 {1000 / desired} ms，不是本次请求的 100 ms。已阻止转发。文件未修改。";
        return new HwFrequenceParse(true, desired, native, rule, ms100, detail);
    }
}

public static class ForwardGate
{
    public static bool Allows(HwFrequenceParse parse, bool acceptedLimits, out string reason)
    {
        if (!parse.SyntaxOk || !parse.CompatibleWith100Ms)
        {
            reason = parse.Detail;
            return false;
        }
        if (!acceptedLimits)
        {
            reason = "尚未确认已知晓单位和新鲜度限制。不能转发。";
            return false;
        }
        reason = parse.Detail;
        return true;
    }
}
