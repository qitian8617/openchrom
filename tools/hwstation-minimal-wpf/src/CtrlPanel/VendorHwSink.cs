using System.IO;
using System.Runtime.InteropServices;

namespace HwMinimal.CtrlPanel;

public sealed class VendorHwSink : IHwSink
{
    private const uint FailCritical = 0x0001;
    private const uint NoOpenError = 0x8000;

    [UnmanagedFunctionPointer(CallingConvention.StdCall)]
    private delegate int SendDataToHW(int data1, int data2, int data3);

    private IntPtr _module;
    private SendDataToHW? _send;
    private int _wedged;

    public bool IsReady => _module != IntPtr.Zero && _send != null && Volatile.Read(ref _wedged) == 0;

    public string Load(string? path)
    {
        if (IntPtr.Size != 4)
        {
            return "当前进程不是 32 位，未加载 DLL。";
        }
        if (string.IsNullOrWhiteSpace(path))
        {
            return "未填写 DLL 路径，未加载。";
        }
        if (!File.Exists(path))
        {
            return "找不到 DLL：" + path + "。没有搜索 D:\\HWGMP，也没有复制文件。";
        }
        if (!TryMachine(path, out int machine, out string error))
        {
            return error;
        }
        if (machine != 0x14C)
        {
            return "不是 32 位 DLL（machine=0x" + machine.ToString("X") + "）。未加载。";
        }
        uint oldMode = SetErrorMode(FailCritical | NoOpenError);
        try
        {
            _module = LoadLibraryW(path);
        }
        finally
        {
            SetErrorMode(oldMode);
        }
        if (_module == IntPtr.Zero)
        {
            return "LoadLibrary 失败，错误 " + Marshal.GetLastWin32Error() + "。未复制 DLL。";
        }
        IntPtr addr = GetProcAddress(_module, "SendDataToHW");
        if (addr == IntPtr.Zero)
        {
            addr = GetProcAddress(_module, "SendDataToHW@12");
        }
        if (addr == IntPtr.Zero)
        {
            return "DLL 中没有 SendDataToHW。";
        }
        _send = Marshal.GetDelegateForFunctionPointer<SendDataToHW>(addr);
        return "";
    }

    public HwCallResult Send(int data1, int data2, int data3)
    {
        if (Volatile.Read(ref _wedged) == 1)
        {
            return new HwCallResult(data1, data2, data3, -1, "DLL 调用已经卡住，没有继续调用。界面没有被这调用占住。");
        }
        SendDataToHW? send = _send;
        if (send == null)
        {
            return new HwCallResult(data1, data2, data3, -1, "DLL 未加载。");
        }
        int code = 0;
        var thread = new Thread(() => code = send(data1, data2, data3)) { IsBackground = true };
        thread.Start();
        if (!thread.Join(1000))
        {
            Volatile.Write(ref _wedged, 1);
            return new HwCallResult(data1, data2, data3, -1, "DLL 调用超过 1 秒。已停止继续转发。不能安全掐断已经进去的调用。");
        }
        string text = code switch
        {
            1 => "返回 1：已向 HWClass 发出消息。这不是采集成功。",
            0 => "返回 0：未找到 HWClass 窗口，消息未发出。",
            _ => "返回 " + code + "：源码只定义 0 和 1。这不是采集成功。"
        };
        return new HwCallResult(data1, data2, data3, code, text);
    }

    private static bool TryMachine(string path, out int machine, out string error)
    {
        machine = 0;
        error = "";
        try
        {
            using FileStream stream = File.OpenRead(path);
            byte[] dos = new byte[64];
            if (stream.Read(dos, 0, 64) != 64 || dos[0] != (byte)'M' || dos[1] != (byte)'Z')
            {
                error = "不是 PE 文件，未加载。";
                return false;
            }
            int offset = dos[0x3C] | (dos[0x3D] << 8) | (dos[0x3E] << 16) | (dos[0x3F] << 24);
            stream.Position = offset;
            byte[] pe = new byte[6];
            if (stream.Read(pe, 0, 6) != 6 || pe[0] != (byte)'P' || pe[1] != (byte)'E')
            {
                error = "不是 PE 文件，未加载。";
                return false;
            }
            machine = pe[4] | (pe[5] << 8);
            return true;
        }
        catch (Exception ex)
        {
            error = "读 DLL 失败：" + ex.Message;
            return false;
        }
    }

    [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    private static extern IntPtr LoadLibraryW(string path);

    [DllImport("kernel32.dll", CharSet = CharSet.Ansi)]
    private static extern IntPtr GetProcAddress(IntPtr module, string name);

    [DllImport("kernel32.dll")]
    private static extern uint SetErrorMode(uint mode);
}
