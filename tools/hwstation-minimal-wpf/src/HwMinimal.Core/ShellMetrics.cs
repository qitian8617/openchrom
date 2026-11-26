namespace HwMinimal;

/// <summary>
/// Client-pixel and track-size math for the fixed-width shell.
/// The content width stays 600 DIP. Height follows the work area and can change.
/// </summary>
public static class ShellMetrics
{
    public const double ContentWidthDip = 600;
    public const double MinClientHeightDip = 520;
    public const int TrackChromePx = 40;

    public static double DpiScale(uint dpi) => dpi >= 96 ? dpi / 96.0 : 1.0;

    public readonly record struct DipSize(double Width, double Height);

    public static DipSize ClientToDip(int widthPx, int heightPx, uint dpi)
    {
        double scale = DpiScale(dpi);
        int width = widthPx > 0 ? widthPx : ClientWidthPx(dpi);
        int height = heightPx > 0 ? heightPx : (int)Math.Round(MinClientHeightDip * scale);
        return new DipSize(width / scale, height / scale);
    }

    public static int ClientWidthPx(uint dpi) => (int)Math.Round(ContentWidthDip * DpiScale(dpi));

    public static int ClientHeightPx(double workAreaHeightDip, uint dpi)
    {
        double heightDip = Math.Max(MinClientHeightDip, workAreaHeightDip - 24);
        return (int)Math.Round(heightDip * DpiScale(dpi));
    }

    public static int TrackMinHeightPx(double contentChromeDip, uint dpi)
    {
        double scale = DpiScale(dpi);
        int contentMin = (int)Math.Round(contentChromeDip * scale);
        int floor = (int)Math.Round(MinClientHeightDip * scale);
        return Math.Max(contentMin, floor) + TrackChromePx;
    }

    public static int TrackMaxHeightPx(double workAreaHeightDip, uint dpi)
        => (int)Math.Round(Math.Max(MinClientHeightDip, workAreaHeightDip) * DpiScale(dpi));
}
