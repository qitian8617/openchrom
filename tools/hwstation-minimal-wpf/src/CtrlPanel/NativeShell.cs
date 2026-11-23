using System.Runtime.InteropServices;
using System.Windows;
using System.Windows.Interop;

namespace HwMinimal.CtrlPanel;

internal static class NativeShell
{
    private const string ClassName = "ControlPanelClass";
    private const string WindowTitle = "色谱反控（最小接入）";
    private const string MutexName = "Local\\HwstationMinimalWpf.ControlPanelClass.instance";
    private const int ErrorAlreadyExists = 183;
    private const uint WM_CLOSE = 0x0010;
    private const uint WM_DESTROY = 0x0002;
    private const uint WM_SIZE = 0x0005;
    private const int WS_OVERLAPPED = 0x00000000;
    private const int WS_CAPTION = 0x00C00000;
    private const int WS_SYSMENU = 0x00080000;
    private const int WS_MINIMIZEBOX = 0x00020000;
    private const int WS_THICKFRAME = 0x00040000;
    private const int WS_FIXED = WS_OVERLAPPED | WS_CAPTION | WS_SYSMENU | WS_MINIMIZEBOX | WS_THICKFRAME;
    private const uint WM_GETMINMAXINFO = 0x0024;
    private const int WS_CHILD = 0x40000000;
    private const int WS_VISIBLE = 0x10000000;
    private const int CW_USEDEFAULT = unchecked((int)0x80000000);

    private static readonly WndProcDelegate Proc = WndProc;
    private static IntPtr _mutex;
    private static IntPtr _background;
    private static IntPtr _hwnd;
    private static HwndSource? _source;
    private static int _trackW;
    private static int _trackMinH;
    private static int _trackMaxH;

    public static bool ClaimOrActivate()
    {
        _mutex = CreateMutexW(IntPtr.Zero, true, MutexName);
        if (_mutex == IntPtr.Zero || Marshal.GetLastWin32Error() == ErrorAlreadyExists)
        {
            IntPtr existing = FindWindowW(ClassName, WindowTitle);
            if (existing != IntPtr.Zero)
            {
                ShowWindow(existing, 9);
                SetForegroundWindow(existing);
            }
            else
            {
                MessageBoxW(IntPtr.Zero, "最小接入面板已经打开，但没有按标题找到 ControlPanelClass。旧的反控程序如果也注册了这个类名，不会被这次启动激活。", "反控", 0);
            }
            if (_mutex != IntPtr.Zero)
            {
                CloseHandle(_mutex);
                _mutex = IntPtr.Zero;
            }
            return false;
        }
        return true;
    }

    public static void Show()
    {
        if (_mutex == IntPtr.Zero)
        {
            return;
        }
        var klass = new WndClass
        {
            lpfnWndProc = Marshal.GetFunctionPointerForDelegate(Proc),
            hInstance = GetModuleHandleW(null),
            hCursor = LoadCursorW(IntPtr.Zero, 32512),
            hbrBackground = WhiteBrush(),
            lpszClassName = ClassName
        };
        RegisterClassW(ref klass);
        uint dpi = GetDpiForSystem();
        double scale = dpi >= 96 ? dpi / 96.0 : 1;
        var work = SystemParameters.WorkArea;
        int clientW = (int)Math.Round(600 * scale);
        int clientH = (int)Math.Round(Math.Max(480, work.Height - 24) * scale);
        var outer = new RectI { Left = 0, Top = 0, Right = clientW, Bottom = clientH };
        AdjustWindowRect(ref outer, WS_FIXED, false);
        _trackW = outer.Right - outer.Left;
        _trackMinH = (int)Math.Round(480 * scale) + 40;
        _trackMaxH = (int)Math.Round(work.Height * scale);
        _hwnd = CreateWindowExW(0, ClassName, WindowTitle, WS_FIXED, CW_USEDEFAULT, CW_USEDEFAULT, _trackW, Math.Min(_trackMaxH, outer.Bottom - outer.Top), IntPtr.Zero, IntPtr.Zero, klass.hInstance, IntPtr.Zero);
        if (_hwnd == IntPtr.Zero)
        {
            MessageBoxW(IntPtr.Zero, "没有创建 ControlPanelClass 窗口，错误 " + Marshal.GetLastWin32Error() + "。WPF 不会自动提供这个类名。", "反控", 0);
            Application.Current?.Shutdown();
            return;
        }
        var parameters = new HwndSourceParameters("HwMinimalContent")
        {
            ParentWindow = _hwnd,
            WindowStyle = WS_CHILD | WS_VISIBLE,
            Width = 600,
            Height = 1024
        };
        _source = new HwndSource(parameters)
        {
            RootVisual = new MainView()
        };
        ResizeChild();
        ShowWindow(_hwnd, 5);
        UpdateWindow(_hwnd);
    }

    private static IntPtr WndProc(IntPtr hwnd, uint msg, IntPtr wParam, IntPtr lParam)
    {
        switch (msg)
        {
            case WM_SIZE:
                ResizeChild();
                return IntPtr.Zero;
            case WM_GETMINMAXINFO:
                LockWidth(lParam);
                return IntPtr.Zero;
            case WM_CLOSE:
                MessageBoxW(hwnd, "关闭本窗口不会主动发送 STOP_ACQ，也不会关闭温控。\n连接断开后，设备按它自己的逻辑结束采集。这不是急停。", "反控", 0);
                DestroyWindow(hwnd);
                return IntPtr.Zero;
            case WM_DESTROY:
                System.Windows.Threading.Dispatcher.CurrentDispatcher.BeginInvoke(() => Application.Current?.Shutdown());
                return IntPtr.Zero;
        }
        return DefWindowProcW(hwnd, msg, wParam, lParam);
    }

    private static void LockWidth(IntPtr lParam)
    {
        var info = Marshal.PtrToStructure<MinMaxInfo>(lParam);
        info.MinTrack.X = _trackW;
        info.MaxTrack.X = _trackW;
        info.MinTrack.Y = _trackMinH;
        info.MaxTrack.Y = Math.Max(_trackMinH, _trackMaxH);
        Marshal.StructureToPtr(info, lParam, false);
    }

    private static void ResizeChild()
    {
        if (_source == null || !GetClientRect(_hwnd, out RectI rect))
        {
            return;
        }
        MoveWindow(_source.Handle, 0, 0, Math.Max(0, rect.Right - rect.Left), Math.Max(0, rect.Bottom - rect.Top), true);
    }

    private delegate IntPtr WndProcDelegate(IntPtr hwnd, uint msg, IntPtr wParam, IntPtr lParam);

    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
    private struct WndClass
    {
        public uint style;
        public IntPtr lpfnWndProc;
        public int cbClsExtra;
        public int cbWndExtra;
        public IntPtr hInstance;
        public IntPtr hIcon;
        public IntPtr hCursor;
        public IntPtr hbrBackground;
        public string? lpszMenuName;
        public string lpszClassName;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct RectI
    {
        public int Left;
        public int Top;
        public int Right;
        public int Bottom;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct MinMaxPoint
    {
        public int X;
        public int Y;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct MinMaxInfo
    {
        public MinMaxPoint Reserved;
        public MinMaxPoint MaxSize;
        public MinMaxPoint MaxPosition;
        public MinMaxPoint MinTrack;
        public MinMaxPoint MaxTrack;
    }

    [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    private static extern IntPtr CreateMutexW(IntPtr attributes, bool initialOwner, string name);

    [DllImport("kernel32.dll")]
    private static extern bool CloseHandle(IntPtr handle);

    [DllImport("kernel32.dll", CharSet = CharSet.Unicode)]
    private static extern IntPtr GetModuleHandleW(string? module);

    [DllImport("user32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    private static extern ushort RegisterClassW(ref WndClass klass);

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    private static extern IntPtr CreateWindowExW(int exStyle, string klass, string windowName, int style, int x, int y, int width, int height, IntPtr parent, IntPtr menu, IntPtr instance, IntPtr param);

    [DllImport("user32.dll")]
    private static extern bool AdjustWindowRect(ref RectI rect, int style, bool menu);

    [DllImport("user32.dll")]
    private static extern uint GetDpiForSystem();

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    private static extern IntPtr FindWindowW(string klass, string windowName);

    [DllImport("user32.dll")]
    private static extern bool ShowWindow(IntPtr hwnd, int command);

    [DllImport("user32.dll")]
    private static extern bool UpdateWindow(IntPtr hwnd);

    [DllImport("user32.dll")]
    private static extern bool DestroyWindow(IntPtr hwnd);

    [DllImport("user32.dll")]
    private static extern bool SetForegroundWindow(IntPtr hwnd);

    [DllImport("user32.dll")]
    private static extern bool GetClientRect(IntPtr hwnd, out RectI rect);

    [DllImport("user32.dll")]
    private static extern bool MoveWindow(IntPtr hwnd, int x, int y, int width, int height, bool repaint);

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    private static extern int MessageBoxW(IntPtr hwnd, string text, string caption, uint type);

    [DllImport("user32.dll")]
    private static extern IntPtr DefWindowProcW(IntPtr hwnd, uint msg, IntPtr wParam, IntPtr lParam);

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    private static extern IntPtr LoadCursorW(IntPtr instance, int cursor);

    [DllImport("gdi32.dll")]
    private static extern IntPtr CreateSolidBrush(uint color);

    private static IntPtr WhiteBrush()
    {
        if (_background == IntPtr.Zero)
        {
            _background = CreateSolidBrush(0x00FFFFFF);
        }
        return _background;
    }
}
