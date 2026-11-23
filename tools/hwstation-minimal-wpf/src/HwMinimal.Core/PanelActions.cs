namespace HwMinimal;

/// <summary>
/// Faces for the single main-screen buttons in MainActivity
/// analysisActionRow, tempControlActionRow, mainFidActionRow.
/// A click does not by itself confirm hardware state.
/// </summary>
public readonly record struct ActionFace(string Text, bool Enabled, string Role)
{
    public bool Sends => Role is PanelActions.Start or PanelActions.Stop or PanelActions.ValveOpen or PanelActions.ValveClose or PanelActions.Ignite;
}

public static class PanelActions
{
    public const string Start = "start";
    public const string Stop = "stop";
    public const string None = "none";
    public const string ValveOpen = "valve-open";
    public const string ValveClose = "valve-close";
    public const string Ignite = "ignite";

    public static ActionFace Analysis(string? phase)
    {
        return phase switch
        {
            "WaitAck" or "WaitPair" => new("进行中", true, Stop),
            "Running" or "Draining" => new("停止", true, Stop),
            "Invalid" => new("启动", true, Start),
            _ => new("启动", true, Start)
        };
    }

    public static string AnalysisStatus(string? phase)
    {
        return phase switch
        {
            "WaitAck" => "进行中：已请求设备采集，尚未通知 HW。",
            "WaitPair" => "进行中：等待真实点。HW 记录未确认。",
            "Running" => "已向 HW 提交取数。这不是记录已确认。",
            "Draining" => "正在停止分析。这不是停热。",
            "Invalid" => "这次分析无效，可以重新启动。",
            _ => "采集状态: 待机"
        };
    }

    public static ActionFace Heat(bool linked, bool? runningConfirmed, bool gasOk)
    {
        if (!linked || runningConfirmed == null)
        {
            return new("停止", true, Stop);
        }
        if (runningConfirmed == true)
        {
            return new("停止", true, Stop);
        }
        if (!gasOk)
        {
            return new("启动", true, None);
        }
        return new("启动", true, Start);
    }

    public static string HeatStatus(bool linked, bool? runningConfirmed)
    {
        if (!linked || runningConfirmed == null)
        {
            return "温控状态未知。停止仍可发送，断连不会记成已关闭。";
        }
        return runningConfirmed == true
            ? "温控已确认启动。停止要等停止应答，才算关闭。"
            : "温控未启动。";
    }

    public static bool? HeatAfterStart(bool? previous, bool gasOk, bool allWriteOk)
    {
        if (!gasOk)
        {
            return previous;
        }
        return allWriteOk ? true : previous;
    }

    public static bool? HeatAfterStop(bool allWriteOk)
    {
        return allWriteOk ? false : null;
    }

    public static ActionFace Valve(bool? openConfirmed)
    {
        if (openConfirmed == null)
        {
            return new("空气阀状态未知", true, ValveClose);
        }
        return openConfirmed.Value
            ? new("关闭空气阀", true, ValveClose)
            : new("打开空气阀", true, ValveOpen);
    }

    public static bool? ValveAfterReply(bool? previous, string reply, string sentRole)
    {
        if (reply == null || reply.IndexOf("WRITE_OK", StringComparison.Ordinal) < 0)
        {
            return previous;
        }
        if (sentRole == ValveOpen)
        {
            return true;
        }
        if (sentRole == ValveClose)
        {
            return false;
        }
        return previous;
    }

    public static bool? ValveAfterRead(bool? previous, string json, bool readReachedDevice)
    {
        if (!readReachedDevice)
        {
            return null;
        }
        if (JsonFields.TryBool(json, "valve", out bool open) || JsonFields.TryBool(json, "valve1", out open))
        {
            return open;
        }
        return previous;
    }

    public static ActionFace Ignition(bool inProgress)
    {
        return inProgress
            ? new("点火中...", false, None)
            : new("执行点火流程", true, Ignite);
    }

    public static bool IgnitionAccepted(string reply)
    {
        if (string.IsNullOrWhiteSpace(reply))
        {
            return false;
        }
        if (reply.Contains("未连接", StringComparison.Ordinal)
            || reply.Contains("未发送", StringComparison.Ordinal)
            || reply.Contains("WRITE_FAIL", StringComparison.Ordinal))
        {
            return false;
        }
        return true;
    }
}
