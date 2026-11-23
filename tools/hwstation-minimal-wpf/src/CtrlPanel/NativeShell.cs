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
    private const int WS_FIXED = WS_OVERLAPPED | WS_CAPTION | WS_SYSMENU | WS_MINIMIZEBOX;
    private const int WS_CHILD = 0x40000000;
    private const int WS_VISIBLE = 0x10000000;
    private const int CW_USEDEFAULT = unchecked((int)0x80000000);

    private static readonly WndProcDelegate Proc = WndProc;
    private static IntPtr _mutex;
    private static IntPtr _background;
    private static IntPtr _hwnd;
    private static HwndSource? _source;

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
        // Content canvas is 600×1024. AdjustWindowRect adds the title bar and border; the window cannot be resized.
        var client = new RectI { Left = 0, Top = 0, Right = 600, Bottom = 1024 + 48 };
        AdjustWindowRect(ref client, WS_FIXED, false);
        _hwnd = CreateWindowExW(0, ClassName, WindowTitle, WS_FIXED, CW_USEDEFAULT, CW_USEDEFAULT, client.Right - client.Left, client.Bottom - client.Top, IntPtr.Zero, IntPtr.Zero, klass.hInstance, IntPtr.Zero);
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
