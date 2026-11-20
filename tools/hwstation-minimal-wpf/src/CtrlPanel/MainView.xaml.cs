using System.IO;
using System.Text;
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
    private DateTime? _watchFrom;
    private TimeSpan _watchDone;
    private readonly System.Windows.Threading.DispatcherTimer _watch = new() { Interval = TimeSpan.FromSeconds(1) };

    public MainView()
    {
        InitializeComponent();
        ProcessText.Text = IntPtr.Size == 4
            ? "本进程是 32 位。顶层窗口类名是手写的 ControlPanelClass，WPF 只是里面的内容，不会自动拥有这个类名。"
            : "本进程不是 32 位，不能加载厂商 DLL。请用 x86 的 CtrlPanel.exe。";
        HostBox.Text = "";
        PortBox.Text = "";
        DllBox.Text = "";
        _watch.Tick += (_, _) => ShowWatch();
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

    private void StartAnalysis_Click(object sender, RoutedEventArgs e)
    {
        if (_link == null)
        {
            Note("未连接，未开始分析。");
            return;
        }
        _link.RequestStartAnalysis(_config, AcceptBox.IsChecked == true);
        Note("已请求开始分析。先等设备确认和第一个真实点，再通知 HW 取数。");
    }

    private void StopAnalysis_Click(object sender, RoutedEventArgs e)
    {
        if (_link == null)
        {
            Note("未连接，未停止分析。这不是停热。");
            return;
        }
        _link.RequestStopAnalysis();
        Note("已请求停止分析。这不是停止温控。");
    }

    private async void StopHeat_Click(object sender, RoutedEventArgs e)
    {
        await SendLine("STOP_INLET_HEAT", null);
        await SendLine("STOP_DETECTOR_HEAT", null);
        await SendLine("STOP_OVEN_CONTROL", null);
    }

    private async void StartHeat_Click(object sender, RoutedEventArgs e)
    {
        if (string.IsNullOrWhiteSpace(InletSet.Text) || string.IsNullOrWhiteSpace(DetSet.Text))
        {
            Note("加热设定是空的，未发送。");
            return;
        }
        string inlet = InletSet.Text.Trim();
        string detector = DetSet.Text.Trim();
        await SendLine("WRITE_ZONE_SELECT", "{\"sel\":\"7\"}");
        await SendLine("WRITE_INLET_TEMP", "{\"setpoint\":\"" + inlet + "\"}");
        await SendLine("WRITE_DETECTOR_TEMP", "{\"setpoint\":\"" + detector + "\"}");
        await SendLine("START_INLET_HEAT", null);
        await SendLine("START_DETECTOR_HEAT", null);
        await SendLine("START_OVEN_CONTROL", null);
    }

    private async void ReadTemps_Click(object sender, RoutedEventArgs e)
    {
        string oven = await SendLine("READ_OVEN_TEMP", null);
        string inlet = await SendLine("READ_INLET_TEMP", null);
        string detector = await SendLine("READ_DETECTOR_TEMP", null);
        ZoneText.Text = "进样口 设定 " + JsonFields.Show(inlet, "setpoint") + " 实测 " + JsonFields.Show(inlet, "actual")
            + "；检测器 设定 " + JsonFields.Show(detector, "setpoint") + " 实测 " + JsonFields.Show(detector, "actual")
            + "；柱箱 设定 " + JsonFields.Show(oven, "setpoint") + " 实测 " + JsonFields.Show(oven, "actual");
    }

    private async void ReadProgram_Click(object sender, RoutedEventArgs e)
    {
        OvenText.Text = await SendLine("READ_OVEN_PROGRAM", null);
    }

    private async void WriteProgram_Click(object sender, RoutedEventArgs e)
    {
        if (string.IsNullOrWhiteSpace(OvenHead.Text) || string.IsNullOrWhiteSpace(OvenRows.Text))
        {
            Note("温程是空的，未发送。");
            return;
        }
        string[] head = OvenHead.Text.Split('/', StringSplitOptions.TrimEntries);
        if (head.Length != 4)
        {
            Note("温程头不是 最高/平衡/稳定秒/循环，未发送。");
            return;
        }
        var rows = new StringBuilder();
        int index = 0;
        foreach (string line in OvenRows.Text.Split('\n', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries))
        {
            string[] part = line.Split(' ', StringSplitOptions.RemoveEmptyEntries);
            if (part.Length != 3 || index >= 8)
            {
                Note("某一阶不是三个数，未发送。");
                return;
            }
            if (index > 0)
            {
                rows.Append(',');
            }
            rows.Append("{\"row\":\"" + index + "\",\"rate\":\"" + part[0] + "\",\"temperature\":\"" + part[1] + "\",\"holdTime\":\"" + part[2] + "\"}");
            index++;
        }
        string json = "{\"maxTemperature\":\"" + head[0] + "\",\"equilibriumTime\":\"" + head[1] + "\",\"equilibriumStableSec\":\"" + head[2] + "\",\"loop\":\"" + head[3] + "\",\"rows\":[" + rows + "]}";
        OvenText.Text = await SendLine("WRITE_OVEN_PROGRAM", json);
    }

    private async void ReadDetector_Click(object sender, RoutedEventArgs e)
    {
        string gas = await SendLine("READ_GAS_PRESSURE", null);
        string fid = await SendLine("READ_FID_STATUS", null);
        bool h2Ok = JsonFields.TryNumber(gas, "h2", out double h2) || JsonFields.TryNumber(gas, "adc1", out h2);
        bool airOk = JsonFields.TryNumber(gas, "air", out double air) || JsonFields.TryNumber(gas, "adc2", out air);
        long now = Environment.TickCount64;
        _link?.NoteGas(h2Ok ? h2 : null, airOk ? air : null, now);
        string? block = GasSupply.BlockReason(h2Ok ? h2 : null, airOk ? air : null, now, now);
        HeatText.Text = block ?? "两路气压在 3 秒内都 ≥ 0.1 MPa，允许启动加热。气路动画条件相同，没有手动确认按钮。";
        HeatStartButton.IsEnabled = block == null;
        DetText.Text = "氢气 " + (h2Ok ? h2.ToString(System.Globalization.CultureInfo.InvariantCulture) : "未读取到")
            + " MPa；空气 " + (airOk ? air.ToString(System.Globalization.CultureInfo.InvariantCulture) : "未读取到")
            + " MPa；火焰 " + JsonFields.Show(fid, "flame") + "；电流 pA " + JsonFields.Show(fid, "currentPa")
            + "。点火应答不代表已经着火。";
    }

    private async void Ignite_Click(object sender, RoutedEventArgs e) => DetText.Text = await SendLine("START_FID_IGNITE", null) + " 火焰以随后的状态读取为准。";
    private async void ValveOn_Click(object sender, RoutedEventArgs e) => DetText.Text = await SendLine("START_FID_VALVES", null);
    private async void ValveOff_Click(object sender, RoutedEventArgs e) => DetText.Text = await SendLine("STOP_FID_VALVES", null);
    private async void AutoOn_Click(object sender, RoutedEventArgs e) => DetText.Text = await SendLine("FID_AUTO_ON", null);
    private async void AutoOff_Click(object sender, RoutedEventArgs e) => DetText.Text = await SendLine("FID_AUTO_OFF", null);

    private async void WriteCalibration_Click(object sender, RoutedEventArgs e)
    {
        if (string.IsNullOrWhiteSpace(InletSet.Text) || string.IsNullOrWhiteSpace(DetSet.Text))
        {
            Note("校准设定是空的，未发送。");
            return;
        }
        await SendLine("WRITE_INLET_TEMP", "{\"setpoint\":\"" + InletSet.Text.Trim() + "\"}");
        await SendLine("WRITE_DETECTOR_TEMP", "{\"setpoint\":\"" + DetSet.Text.Trim() + "\"}");
    }

    private void Enroll_Click(object sender, RoutedEventArgs e) => Note(_link == null ? "未连接。" : _link.Engineer.Enroll(EngineerPassword.Password));
    private void Unlock_Click(object sender, RoutedEventArgs e) => Note(_link == null ? "未连接。" : _link.Engineer.Unlock(EngineerPassword.Password));
    private void Lock_Click(object sender, RoutedEventArgs e)
    {
        _link?.Engineer.Lock();
        Note("工程师授权已锁定。");
    }

    private async void ReadPid_Click(object sender, RoutedEventArgs e)
    {
        string json = await SendLine("READ_OVEN_PID", null);
        PidBox.Text = JsonFields.Show(json, "kp") + " " + JsonFields.Show(json, "ki") + " " + JsonFields.Show(json, "kd");
    }

    private async void WritePid_Click(object sender, RoutedEventArgs e)
    {
        string[] part = PidBox.Text.Split(' ', StringSplitOptions.RemoveEmptyEntries);
        if (part.Length != 3)
        {
            Note("PID 不是三个数，未发送。");
            return;
        }
        await SendLine("WRITE_OVEN_PID", "{\"kp\":\"" + part[0] + "\",\"ki\":\"" + part[1] + "\",\"kd\":\"" + part[2] + "\"}");
    }

    private async void ReadStatus_Click(object sender, RoutedEventArgs e)
    {
        LinkText.Text = await SendLine("READ_CONTROL_STATUS", null);
    }

    private void WatchStart_Click(object sender, RoutedEventArgs e)
    {
        _watchFrom = DateTime.UtcNow;
        _watch.Start();
    }

    private void WatchStop_Click(object sender, RoutedEventArgs e) => _watch.Stop();

    private void WatchReset_Click(object sender, RoutedEventArgs e)
    {
        _watch.Stop();
        _watchFrom = null;
        _watchDone = TimeSpan.Zero;
        WatchText.Text = "00:00";
    }

    private void ShowWatch()
    {
        TimeSpan elapsed = _watchFrom == null ? _watchDone : DateTime.UtcNow - _watchFrom.Value;
        WatchText.Text = elapsed.ToString(@"mm\:ss", System.Globalization.CultureInfo.InvariantCulture);
    }

    private async Task<string> SendLine(string verb, string? payload)
    {
        if (_link == null)
        {
            Note("未连接，未发送 " + verb + "。");
            return "未连接";
        }
        string reply = await _link.ExchangeAsync(verb, payload, CancellationToken.None);
        Note(verb + " → " + reply);
        return reply;
    }

    private void Note(string text)
    {
        SetStatus(text);
        Events.Items.Insert(0, text);
        while (Events.Items.Count > 40)
        {
            Events.Items.RemoveAt(Events.Items.Count - 1);
        }
    }
}
