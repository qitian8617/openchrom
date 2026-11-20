using System.IO;
using System.Windows;
using System.Windows.Controls;
using Microsoft.Win32;

namespace HwMinimal.CtrlPanel;

public partial class MainView : UserControl
{
    private readonly VendorHwSink _sink = new();
    private readonly ObservedSample[] _ring = new ObservedSample[30];
    private readonly object _ringGate = new();
    private int _ringCount;
    private int _ringGeneration;
    private int _uiPosted;
    private bool _armWhenLinked;
    private readonly object _life = new();
    private LiveLink? _link;
    private TcpByteTransport? _transport;
    private CancellationTokenSource? _run;
    private HwFrequenceParse _config;

    public MainView()
    {
        InitializeComponent();
        ProcessText.Text = IntPtr.Size == 4
            ? "本进程是 32 位。顶层窗口类名是手写的 ControlPanelClass，WPF 只是里面的内容，不会自动拥有这个类名。"
            : "本进程不是 32 位，不能加载厂商 DLL。请用 x86 的 CtrlPanel.exe。";
        HostBox.Text = "";
        PortBox.Text = "";
        DllBox.Text = "";
    }

    private void Connect_Click(object sender, RoutedEventArgs e)
    {
        if (_run != null)
        {
            SetStatus("已经在连接或已连上。未再次连接。");
            return;
        }
        if (!int.TryParse(PortBox.Text.Trim(), out int port))
        {
            SetStatus("端口是空的或不是整数，未连接。");
            return;
        }
        var cts = new CancellationTokenSource();
        lock (_life)
        {
            _run = cts;
        }
        _ = ConnectAsync(HostBox.Text, port, cts);
    }

    private async Task ConnectAsync(string host, int port, CancellationTokenSource cts)
    {
        try
        {
            TcpByteTransport transport = await TcpByteTransport.ConnectAsync(host, port, cts.Token);
            var link = new LiveLink(transport, _sink);
            link.Status += OnStatus;
            link.Sample += OnSample;
            bool abandoned = false;
            lock (_life)
            {
                if (_run != cts)
                {
                    abandoned = true;
                }
                else
                {
                    _transport = transport;
                    _link = link;
                }
            }
            if (abandoned)
            {
                await transport.DisposeAsync();
                return;
            }
            if (_armWhenLinked)
            {
                link.TryArm(_config, true, out _);
            }
            SetStatus("TCP 已连上，正在等 HELLO_OK。这还不是采集成功。");
            await link.RunAsync(cts.Token);
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            SetStatus("没有连上：" + ex.Message);
        }
        finally
        {
            await ReleaseAsync();
        }
    }

    private async void Disconnect_Click(object sender, RoutedEventArgs e)
    {
        await ReleaseAsync();
        SetStatus("已断开。没有发送加热或点火命令。");
    }

    private void StartAcq_Click(object sender, RoutedEventArgs e)
    {
        if (_link == null)
        {
            SetStatus("还没有连接，未发送 START_ACQ。");
            return;
        }
        _link.RequestDeviceStart();
    }

    private void StopAcq_Click(object sender, RoutedEventArgs e)
    {
        if (_link == null)
        {
            SetStatus("还没有连接，未发送 STOP_ACQ。停止采集不是急停，这里也没有温控可关。");
            return;
        }
        _link.RequestDeviceStop();
    }

    private void PickConfig_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new OpenFileDialog
        {
            Filter = "文本|*.txt|所有文件|*.*",
            CheckFileExists = true
        };
        if (dialog.ShowDialog() != true)
        {
            return;
        }
        try
        {
            string text = File.ReadAllText(dialog.FileName);
            _config = HwFrequence.Parse(text);
            ConfigText.Text = dialog.FileName + "：" + _config.Detail;
        }
        catch (Exception ex)
        {
            _config = default;
            ConfigText.Text = "没有读成：" + ex.Message;
        }
    }

    private void PickDll_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new OpenFileDialog
        {
            Filter = "DLL|*.dll|所有文件|*.*",
            CheckFileExists = true
        };
        if (dialog.ShowDialog() == true)
        {
            DllBox.Text = dialog.FileName;
        }
    }

    private void Arm_Click(object sender, RoutedEventArgs e)
    {
        if (!ForwardGate.Allows(_config, AcceptBox.IsChecked == true, out string reason))
        {
            SetStatus(reason);
            DllText.Text = reason;
            return;
        }
        if (!_sink.IsReady)
        {
            string loaded = _sink.Load(DllBox.Text);
            if (loaded.Length != 0)
            {
                DllText.Text = loaded;
                SetStatus(loaded);
                return;
            }
        }
        _armWhenLinked = true;
        if (_link == null)
        {
            DllText.Text = "DLL 已按填写的路径加载。连接并开始采集后才会转发样本。没有复制 DLL。";
            return;
        }
        if (!_link.TryArm(_config, true, out reason))
        {
            DllText.Text = reason;
            SetStatus(reason);
            return;
        }
        DllText.Text = reason + " 返回 1 不是采集成功。";
    }

    private void Disarm_Click(object sender, RoutedEventArgs e)
    {
        _armWhenLinked = false;
        _link?.DisarmSamples();
        SetStatus("已停止转发样本。DLL 可以仍留在进程里，以便发送工作站停止。");
    }

    private void HwStart_Click(object sender, RoutedEventArgs e) => _link?.RequestHwRecordStart();
    private void HwStop_Click(object sender, RoutedEventArgs e) => _link?.RequestHwRecordStop();
    private void HwZero_Click(object sender, RoutedEventArgs e) => _link?.RequestHwZeroA();

    private void OnStatus(string text)
    {
        Dispatcher.BeginInvoke(() => SetStatus(text));
    }

    private void OnSample(ObservedSample sample)
    {
        lock (_ringGate)
        {
            if (_ringCount < _ring.Length)
            {
                _ring[_ringCount++] = sample;
            }
            else
            {
                Array.Copy(_ring, 1, _ring, 0, _ring.Length - 1);
                _ring[^1] = sample;
            }
            _ringGeneration++;
        }
        if (Interlocked.CompareExchange(ref _uiPosted, 1, 0) != 0)
        {
            return;
        }
        Dispatcher.BeginInvoke(FlushSamples);
    }

    private void FlushSamples()
    {
        ObservedSample[] copy;
        int seen;
        lock (_ringGate)
        {
            copy = _ring.Take(_ringCount).ToArray();
            seen = _ringGeneration;
        }
        Samples.Items.Clear();
        foreach (ObservedSample sample in copy)
        {
            Samples.Items.Add($"{sample.TimeMs} ms   Y={sample.MilliVolts} mV   {sample.FemtoAmps} fA   {(sample.Forwarded ? "已转发" : "仅显示")}");
        }
        Interlocked.Exchange(ref _uiPosted, 0);
        bool more;
        lock (_ringGate)
        {
            more = _ringGeneration != seen;
        }
        if (more && Interlocked.CompareExchange(ref _uiPosted, 1, 0) == 0)
        {
            Dispatcher.BeginInvoke(FlushSamples);
        }
    }

    private void SetStatus(string text)
    {
        StatusText.Text = text;
        if (text.Contains("DLL", StringComparison.Ordinal) || text.Contains("返回", StringComparison.Ordinal))
        {
            DllText.Text = text;
        }
    }

    private async Task ReleaseAsync()
    {
        CancellationTokenSource? run;
        TcpByteTransport? transport;
        lock (_life)
        {
            run = _run;
            _run = null;
            transport = _transport;
            _transport = null;
            _link = null;
        }
        if (run != null)
        {
            await run.CancelAsync();
            run.Dispose();
        }
        if (transport != null)
        {
            await transport.DisposeAsync();
        }
    }
}
