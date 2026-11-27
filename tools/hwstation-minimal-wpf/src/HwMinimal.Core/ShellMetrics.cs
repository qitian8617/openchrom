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

    public readonly record struct PixelSize(int Width, int Height);

    public readonly record struct FirstShowSizes(PixelSize Temporary, PixelSize Final);

    /// <summary>
    /// A one-pixel height change and the exact size to restore.
    /// Showing a window that is already this size does not send WM_SIZE.
    /// </summary>
    public static FirstShowSizes FirstShowNudge(int widthPx, int heightPx)
    {
        int width = Math.Max(1, widthPx);
        int height = Math.Max(1, heightPx);
        int temporary = height > 1 ? height - 1 : height + 1;
        return new FirstShowSizes(new PixelSize(width, temporary), new PixelSize(width, height));
    }

    public static nint MakeSizeLParam(int widthPx, int heightPx)
    {
        uint packed = ((uint)heightPx & 0xFFFF) << 16 | ((uint)widthPx & 0xFFFF);
        return unchecked((nint)packed);
    }
}
