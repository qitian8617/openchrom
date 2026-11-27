using System.Globalization;

namespace HwMinimal;

/// <summary>
/// Homepage temperature cards in the shape of Android MainActivity.compactTempCard.
/// Status is display only. It does not stop heat.
/// </summary>
public static class TempCards
{
    public enum Role
    {
        Detector,
        Inlet,
        Oven
    }

    public readonly record struct Face(
        string Actual,
        string Setpoint,
        string Status,
        string StatusColor,
        string Fill,
        string Stroke);

    public static string FormatActual(double? celsius)
    {
        if (celsius == null || !double.IsFinite(celsius.Value))
        {
            return "\u2014";
        }
        return celsius.Value.ToString("0.0", CultureInfo.InvariantCulture) + " \u2103";
    }

    public static string FormatSetpoint(double? celsius)
    {
        if (celsius == null || !double.IsFinite(celsius.Value))
        {
            return "\u8bbe\u5b9a\u2103 \u2014";
        }
        return "\u8bbe\u5b9a\u2103 " + celsius.Value.ToString("0.0", CultureInfo.InvariantCulture);
    }

    public static Face Describe(Role role, string? json, bool? heatOn)
    {
        json ??= "";
        double? actual = Number(json, "actual");
        double? setpoint = Number(json, "setpoint");
        bool fault = Fault(json);
        string status;
        string statusColor;
        string fill;
        string stroke;
        if (fault)
        {
            status = "\u6545\u969c";
            (fill, stroke, statusColor) = ("#FFF0F0", "#EB5384", "#EB5384");
        }
        else if (heatOn == false)
        {
            status = "\u72b6\u6001: \u505c\u6b62\u6052\u6e29";
            statusColor = "#929DAD";
            if (role == Role.Detector)
            {
                fill = "#FFF7E8";
                stroke = "#D68910";
            }
            else
            {
                fill = "#EEF6FB";
                stroke = "#5A9BC8";
            }
        }
        else if (heatOn != true || (role == Role.Oven ? !OvenKnown(json, actual, setpoint) : actual == null || setpoint == null))
        {
            status = "\u72b6\u6001\u672a\u77e5";
            (fill, stroke, statusColor) = ("#EEF0F3", "#7E8C9E", "#FF7018");
        }
        else if (role == Role.Oven)
        {
            (status, fill, stroke, statusColor) = OvenFace(json, actual, setpoint);
        }
        else
        {
            (status, fill, stroke, statusColor) = ZoneFace(actual!.Value, setpoint!.Value);
        }
        return new Face(FormatActual(actual), FormatSetpoint(setpoint), status, statusColor, fill, stroke);
    }

    private static (string Status, string Fill, string Stroke, string Color) ZoneFace(double actual, double setpoint)
    {
        double delta = Math.Abs(actual - setpoint);
        if (delta <= 3.0)
        {
            return ("\u6052\u6e29\u72b6\u6001", "#E8F8ED", "#1ABC5E", "#1ABC5E");
        }
        if (actual < setpoint)
        {
            return ("\u5347\u6e29\u72b6\u6001", "#FFF3E0", "#FF7018", "#FF7018");
        }
        return ("\u964d\u6e29\u72b6\u6001", "#E8F2FC", "#2F6FEB", "#2F6FEB");
    }

    private static (string Status, string Fill, string Stroke, string Color) OvenFace(string json, double? actual, double? setpoint)
    {
        int? mode = Number(json, "mode") is double m ? (int)m : null;
        if (actual != null && setpoint != null && Math.Abs(actual.Value - setpoint.Value) <= 0.5 && mode != 3)
        {
            return ("\u6052\u6e29\u72b6\u6001", "#E8F8ED", "#1ABC5E", "#1ABC5E");
        }
        if (mode == 1)
        {
            return ("\u5347\u6e29\u72b6\u6001", "#FFF3E0", "#FF7018", "#FF7018");
        }
        if (mode == 3)
        {
            return ("\u964d\u6e29\u72b6\u6001", "#E8F2FC", "#2F6FEB", "#2F6FEB");
        }
        if (actual != null && setpoint != null)
        {
            return actual.Value < setpoint.Value
                ? ("\u5347\u6e29\u72b6\u6001", "#FFF3E0", "#FF7018", "#FF7018")
                : ("\u964d\u6e29\u72b6\u6001", "#E8F2FC", "#2F6FEB", "#2F6FEB");
        }
        return ("\u72b6\u6001\u672a\u77e5", "#EEF0F3", "#7E8C9E", "#FF7018");
    }

    private static bool OvenKnown(string json, double? actual, double? setpoint)
    {
        if (Number(json, "valid") is 0)
        {
            return false;
        }
        return Number(json, "mode") != null || (actual != null && setpoint != null);
    }

    private static bool Fault(string json)
    {
        if (Number(json, "fault") is double fault && fault != 0)
        {
            return true;
        }
        return false;
    }

    private static double? Number(string json, string key)
    {
        return JsonFields.TryNumber(json, key, out double value) ? value : null;
    }
}
