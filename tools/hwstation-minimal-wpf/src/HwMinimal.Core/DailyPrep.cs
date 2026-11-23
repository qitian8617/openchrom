using System.Globalization;
using System.Security.Cryptography;
using System.Text;

namespace HwMinimal;

public sealed class PanelSettings
{
    public const string DefaultHost = "192.168.1.30";
    public const int DefaultPort = 8234;
    public const string DefaultHwHome = @"D:\HWGMP\program";
    public const double ContentWidth = 600;

    public string Host { get; set; } = DefaultHost;
    public int Port { get; set; } = DefaultPort;
    public string HwHome { get; set; } = "";
    public string DllPath { get; set; } = "";
    public bool RiskAcknowledged { get; set; }

    public string EffectiveHome => string.IsNullOrWhiteSpace(HwHome) ? DefaultHwHome : HwHome.Trim();

    public string StartupStatus()
    {
        return "未连接。已保存地址 " + Host + ":" + Port.ToString(CultureInfo.InvariantCulture) + "。启动不会自动连接，也不会发写命令。";
    }

    public static PanelSettings Load(string path)
    {
        var settings = new PanelSettings();
        if (!File.Exists(path))
        {
            return settings;
        }
        foreach (string raw in File.ReadAllLines(path))
        {
            int eq = raw.IndexOf('=');
            if (eq <= 0)
            {
                continue;
            }
            string key = raw[..eq].Trim();
            string value = raw[(eq + 1)..].Trim();
            switch (key)
            {
                case "host" when value.Length > 0:
                    settings.Host = value;
                    break;
                case "port" when int.TryParse(value, NumberStyles.None, CultureInfo.InvariantCulture, out int port) && port is >= 1 and <= 65535:
                    settings.Port = port;
                    break;
                case "hwHome":
                    settings.HwHome = value;
                    break;
                case "dll":
                    settings.DllPath = value;
                    break;
                case "risk" when value == "1":
                    settings.RiskAcknowledged = true;
                    break;
            }
        }
        return settings;
    }

    public void Save(string path)
    {
        Directory.CreateDirectory(Path.GetDirectoryName(Path.GetFullPath(path))!);
        string text = "host=" + Host.Trim() + "\n"
            + "port=" + Port.ToString(CultureInfo.InvariantCulture) + "\n"
            + "hwHome=" + HwHome.Trim() + "\n"
            + "dll=" + DllPath.Trim() + "\n"
            + "risk=" + (RiskAcknowledged ? "1" : "0") + "\n";
        File.WriteAllText(path, text);
    }
}

public static class VendorDllIdentity
{
    public const int Size = 98304;
    public const string Sha256 = "9b5c7d6a4a2a2e9dec55e656f2a8dcf9d32986b4bed06daf935dab11dc2e99ba";
    public const string FileName = "HWSendData.dll";

    public static string FixedPath(string appDirectory) => Path.Combine(appDirectory, FileName);

    public static string Describe(string? path)
    {
        if (string.IsNullOrWhiteSpace(path) || !File.Exists(path))
        {
            return "缺少已核对的厂商 HWSendData.dll。本仓库没有这份原件，不能用替身冒充。未搜索 PATH，也未修改 HW 安装目录。";
        }
        byte[] bytes = File.ReadAllBytes(path);
        string hash = Convert.ToHexString(SHA256.HashData(bytes)).ToLowerInvariant();
        if (bytes.Length != Size || hash != Sha256)
        {
            return "该文件不是已核对的 32 位厂商 DLL（期望 98304 字节，SHA256 " + Sha256 + "）。未加载。";
        }
        return "";
    }
}

public readonly record struct DailyPrepResult(bool Ok, HwFrequenceParse Frequence, string DllPath, string Detail);

public static class DailyPrep
{
    public static DailyPrepResult Evaluate(PanelSettings settings, bool frequenceFileExists, string? frequenceText, string? dllPath)
    {
        if (!settings.RiskAcknowledged)
        {
            return new DailyPrepResult(false, default, "", "尚未记录单位和新鲜度确认。请在设置里确认一次。风险说明仍会一直显示，未开始分析。");
        }
        if (string.IsNullOrWhiteSpace(settings.Host) || settings.Port is < 1 or > 65535)
        {
            return new DailyPrepResult(false, default, "", "保存的主机或端口无效，未连接。");
        }
        if (!frequenceFileExists || frequenceText == null)
        {
            return new DailyPrepResult(false, default, "", "没有读到 " + settings.EffectiveHome + "\\HWFrequence.txt。未使用项目里的 10,10 候选文件，也未改安装配置。");
        }
        HwFrequenceParse parsed = HwFrequence.Parse(frequenceText);
        if (!parsed.CompatibleWith100Ms)
        {
            return new DailyPrepResult(false, parsed, "", parsed.Detail + " 这是安装目录里的实际文件，未修改。");
        }
        string dll = dllPath ?? "";
        string dllError = VendorDllIdentity.Describe(dll);
        if (dllError.Length != 0)
        {
            return new DailyPrepResult(false, parsed, dll, dllError);
        }
        return new DailyPrepResult(true, parsed, dll, "频率与 100 ms 一致，DLL 与已核对原件一致。未修改安装配置。");
    }
}
