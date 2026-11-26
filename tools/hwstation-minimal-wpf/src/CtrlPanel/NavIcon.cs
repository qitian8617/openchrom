using System.Windows;
using System.Windows.Media;

namespace HwMinimal.CtrlPanel;

/// <summary>
/// The six strokes from MainActivity.NavIconView. Active fills a blue rounded square and draws white.
/// </summary>
public sealed class NavIcon : FrameworkElement
{
    public static readonly DependencyProperty TypeProperty = DependencyProperty.Register(
        nameof(Type), typeof(int), typeof(NavIcon), new FrameworkPropertyMetadata(0, FrameworkPropertyMetadataOptions.AffectsRender));

    public static readonly DependencyProperty ActiveProperty = DependencyProperty.Register(
        nameof(Active), typeof(bool), typeof(NavIcon), new FrameworkPropertyMetadata(false, FrameworkPropertyMetadataOptions.AffectsRender));

    public int Type
    {
        get => (int)GetValue(TypeProperty);
        set => SetValue(TypeProperty, value);
    }

    public bool Active
    {
        get => (bool)GetValue(ActiveProperty);
        set => SetValue(ActiveProperty, value);
    }

    protected override void OnRender(DrawingContext dc)
    {
        double s = Math.Min(ActualWidth, ActualHeight);
        if (s <= 0)
        {
            return;
        }
        var stroke = new SolidColorBrush(Active ? Colors.White : Color.FromRgb(0x9C, 0xA7, 0xB5));
        stroke.Freeze();
        var pen = new Pen(stroke, s * 0.07)
        {
            StartLineCap = PenLineCap.Round,
            EndLineCap = PenLineCap.Round,
            LineJoin = PenLineJoin.Round
        };
        pen.Freeze();
        if (Active)
        {
            var fill = new SolidColorBrush(Color.FromRgb(0x2F, 0x6F, 0xEB));
            fill.Freeze();
            dc.DrawRoundedRectangle(fill, null, new Rect(0, 0, ActualWidth, ActualHeight), s * 0.12, s * 0.12);
        }
        switch (Type)
        {
            case 0:
                DrawHome(dc, pen, s);
                break;
            case 1:
                DrawHeat(dc, pen, s);
                break;
            case 2:
                DrawDetector(dc, pen, s);
                break;
            case 3:
                DrawClipboard(dc, pen, s);
                break;
            case 4:
                DrawTimer(dc, pen, s);
                break;
            default:
                DrawGear(dc, pen, s);
                break;
        }
    }

    private static void DrawHome(DrawingContext dc, Pen pen, double s)
    {
        var roof = new StreamGeometry();
        using (StreamGeometryContext ctx = roof.Open())
        {
            ctx.BeginFigure(P(s, 0.18, 0.48), false, false);
            ctx.LineTo(P(s, 0.5, 0.18), true, true);
            ctx.LineTo(P(s, 0.82, 0.48), true, true);
        }
        roof.Freeze();
        dc.DrawGeometry(null, pen, roof);
        dc.DrawRectangle(null, pen, new Rect(s * 0.27, s * 0.47, s * 0.46, s * 0.31));
        dc.DrawLine(pen, P(s, 0.48, 0.78), P(s, 0.48, 0.62));
    }

    private static void DrawHeat(DrawingContext dc, Pen pen, double s)
    {
        for (int i = 0; i < 3; i++)
        {
            double x = s * (0.32 + i * 0.16);
            var flame = new StreamGeometry();
            using (StreamGeometryContext ctx = flame.Open())
            {
                ctx.BeginFigure(new Point(x, s * 0.78), false, false);
                ctx.BezierTo(new Point(x - s * 0.14, s * 0.60), new Point(x + s * 0.14, s * 0.52), new Point(x, s * 0.34), true, true);
                ctx.BezierTo(new Point(x - s * 0.06, s * 0.26), new Point(x - s * 0.02, s * 0.19), new Point(x + s * 0.05, s * 0.13), true, true);
            }
            flame.Freeze();
            dc.DrawGeometry(null, pen, flame);
        }
    }

    private static void DrawDetector(DrawingContext dc, Pen pen, double s)
    {
        dc.PushTransform(new RotateTransform(-38, s * 0.5, s * 0.5));
        dc.DrawRoundedRectangle(null, pen, new Rect(s * 0.32, s * 0.15, s * 0.33, s * 0.60), s * 0.05, s * 0.05);
        dc.DrawLine(pen, P(s, 0.32, 0.30), P(s, 0.65, 0.30));
        dc.Pop();
        dc.DrawLine(pen, P(s, 0.22, 0.78), P(s, 0.42, 0.78));
    }

    private static void DrawClipboard(DrawingContext dc, Pen pen, double s)
    {
        dc.DrawRoundedRectangle(null, pen, new Rect(s * 0.25, s * 0.18, s * 0.50, s * 0.64), s * 0.05, s * 0.05);
        dc.DrawRoundedRectangle(null, pen, new Rect(s * 0.38, s * 0.12, s * 0.24, s * 0.13), s * 0.04, s * 0.04);
        dc.DrawLine(pen, P(s, 0.36, 0.42), P(s, 0.64, 0.42));
        dc.DrawLine(pen, P(s, 0.36, 0.56), P(s, 0.64, 0.56));
    }

    private static void DrawTimer(DrawingContext dc, Pen pen, double s)
    {
        dc.DrawEllipse(null, pen, P(s, 0.5, 0.52), s * 0.28, s * 0.28);
        dc.DrawLine(pen, P(s, 0.5, 0.52), P(s, 0.5, 0.34));
        dc.DrawLine(pen, P(s, 0.5, 0.52), P(s, 0.64, 0.58));
        dc.DrawLine(pen, P(s, 0.42, 0.16), P(s, 0.58, 0.16));
    }

    private static void DrawGear(DrawingContext dc, Pen pen, double s)
    {
        dc.DrawEllipse(null, pen, P(s, 0.5, 0.5), s * 0.20, s * 0.20);
        dc.DrawEllipse(null, pen, P(s, 0.5, 0.5), s * 0.08, s * 0.08);
        for (int i = 0; i < 8; i++)
        {
            double a = Math.PI * i / 4.0;
            dc.DrawLine(
                pen,
                new Point(s * 0.5 + Math.Cos(a) * s * 0.26, s * 0.5 + Math.Sin(a) * s * 0.26),
                new Point(s * 0.5 + Math.Cos(a) * s * 0.36, s * 0.5 + Math.Sin(a) * s * 0.36));
        }
    }

    private static Point P(double s, double x, double y) => new(s * x, s * y);
}
