namespace HwMinimal;

/// <summary>
/// Android main canvas mapped from the 2026-11-10 MainActivity lines that were actually read,
/// plus MAIN-FLOW-SCHEMATIC.md (600×1024 portrait, FID top-left, inlet top-right, column bottom-center).
/// Canvas height is dp(430). Schematic parent padding is dp(10) and its horizontal margin is dp(12),
/// so a 600dp panel gives a 556dp canvas. FID and inlet are 140×200. deviceOuter = 4+112+18 = 134.
/// Column placeholder is 150×148, bottom-centered, bottom margin 6. scheduleSchematicPlacement was
/// not in the extracted source, so the later oven resize is not copied.
/// Outlet pixels are opaque samples of the nodpi bitmaps (alpha &gt; 20), mapped with the same
/// uniform fit an ImageView uses. The view rectangle is not the outlet.
/// </summary>
public static class SchematicLayout
{
    public readonly record struct Box(double X, double Y, double W, double H)
    {
        public double Right => X + W;
        public double Bottom => Y + H;
    }

    public readonly record struct Pt(double X, double Y);

    public const double ScreenWidth = 600;
    public const double Width = 556;
    public const double Height = 430;
    public const double TabHeader = 28;
    public const double TopChrome = 28;
    public const double BottomChrome = 248;
    /// <summary>gc_column.png glyphs for 色谱柱 start after a clear gap. y=305 is empty; y=320 is ink.</summary>
    public const double ColumnGlyphTop = 305;

    public const double FidPixelsW = 480;
    public const double FidPixelsH = 640;
    public const double InletPixelsW = 480;
    public const double InletPixelsH = 640;
    public const double ColumnPixelsW = 349;
    public const double ColumnPixelsH = 346;

    public static readonly Box Fid = new(134, 16, 140, 200);
    public static readonly Box Inlet = new(Width - 134 - 140, 16, 140, 200);
    public static readonly Box Column = new((Width - 150) / 2.0, Height - 6 - 148, 150, 148);
    public static readonly Box DetectorCard = new(4, 28, 112, 78);
    public static readonly Box InletCard = new(Width - 4 - 112, 28, 112, 78);
    public static readonly Box OvenCard = new(4, 280, 136, 72);
    public static readonly Box FanCard = new(Width - 4 - 136, 280, 136, 118);
    public static readonly Box VentCard = new(Width - 4 - 112, 162, 112, 108);

    public static Pt FidOutlet => MapUniform(Fid, FidPixelsW, FidPixelsH, 287, 605);
    public static Pt InletOutlet => MapUniform(Inlet, InletPixelsW, InletPixelsH, 211, 605);
    public static Pt ColumnLeft => MapUniform(Column, ColumnPixelsW, ColumnPixelsH, 5.493, 240.885);
    public static Pt ColumnRight => MapUniform(Column, ColumnPixelsW, ColumnPixelsH, 342.518, 240.104);

    public static Pt[] LeftPipe => Elbow(FidOutlet, ColumnLeft, left: true);
    public static Pt[] RightPipe => Elbow(InletOutlet, ColumnRight, left: false);

    public static Pt MapUniform(Box box, double imgW, double imgH, double px, double py)
    {
        double scale = Math.Min(box.W / imgW, box.H / imgH);
        double contentW = imgW * scale;
        double contentH = imgH * scale;
        double originX = box.X + (box.W - contentW) / 2.0;
        double originY = box.Y + (box.H - contentH) / 2.0;
        return new Pt(originX + px * scale, originY + py * scale);
    }

    public static bool Separated(Box a, Box b)
    {
        return a.Right <= b.X || b.Right <= a.X || a.Bottom <= b.Y || b.Bottom <= a.Y;
    }

    public static bool Contains(Box outer, Box inner)
    {
        return inner.X >= outer.X && inner.Y >= outer.Y && inner.Right <= outer.Right && inner.Bottom <= outer.Bottom;
    }

    public static bool SegmentHits(Pt a, Pt b, Box box)
    {
        double dx = b.X - a.X;
        double dy = b.Y - a.Y;
        double t0 = 0;
        double t1 = 1;
        return Clip(-dx, a.X - box.X, ref t0, ref t1)
            && Clip(dx, box.Right - a.X, ref t0, ref t1)
            && Clip(-dy, a.Y - box.Y, ref t0, ref t1)
            && Clip(dy, box.Bottom - a.Y, ref t0, ref t1);
    }

    public static bool PipesClearCards()
    {
        Box[] cards = { DetectorCard, InletCard, OvenCard, FanCard, VentCard };
        return Clear(LeftPipe, cards) && Clear(RightPipe, cards);
    }

    public static bool ButtonsClear(double clientHeight)
    {
        double middleTop = TabHeader + TopChrome;
        double buttonTop = clientHeight - BottomChrome;
        double buttonBottom = clientHeight;
        bool bands = buttonTop >= middleTop && buttonBottom <= clientHeight && buttonTop < buttonBottom;
        bool wide = 62 + 8 + 62 <= ScreenWidth - 16 && 150 <= ScreenWidth - 32;
        bool cardsInside = Contains(new Box(0, 0, Width, Height), DetectorCard)
            && Contains(new Box(0, 0, Width, Height), InletCard)
            && Contains(new Box(0, 0, Width, Height), OvenCard)
            && Contains(new Box(0, 0, Width, Height), FanCard)
            && Contains(new Box(0, 0, Width, Height), VentCard);
        double middle = buttonTop - middleTop;
        bool canvasNotStretched = Height == 430 && middle > 0;
        return bands && wide && cardsInside && canvasNotStretched;
    }

    private static bool Clear(Pt[] pipe, Box[] cards)
    {
        for (int i = 1; i < pipe.Length; i++)
        {
            foreach (Box card in cards)
            {
                if (SegmentHits(pipe[i - 1], pipe[i], card))
                {
                    return false;
                }
            }
        }
        return true;
    }

    private static Pt[] Elbow(Pt outlet, Pt columnEnd, bool left)
    {
        double x = left
            ? Math.Min(outlet.X, columnEnd.X) - 16
            : Math.Max(outlet.X, columnEnd.X) + 16;
        if (left)
        {
            x = Math.Max(x, OvenCard.Right + 6);
        }
        else
        {
            x = Math.Min(x, Math.Min(FanCard.X, VentCard.X) - 6);
        }
        return new[] { outlet, new Pt(x, outlet.Y), new Pt(x, columnEnd.Y), columnEnd };
    }

    private static bool Clip(double p, double q, ref double t0, ref double t1)
    {
        if (Math.Abs(p) < 1e-9)
        {
            return q >= 0;
        }
        double t = q / p;
        if (p < 0)
        {
            if (t > t1)
            {
                return false;
            }
            if (t > t0)
            {
                t0 = t;
            }
        }
        else
        {
            if (t < t0)
            {
                return false;
            }
            if (t < t1)
            {
                t1 = t;
            }
        }
        return true;
    }
}
