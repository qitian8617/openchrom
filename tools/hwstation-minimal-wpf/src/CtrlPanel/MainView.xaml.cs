using System.IO;
using System.Text;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
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
    private PanelSettings _settings = new();
    private DateTime? _watchFrom;
    private TimeSpan _watchDone;
    private readonly System.Windows.Threading.DispatcherTimer _watch = new() { Interval = TimeSpan.FromSeconds(1) };
    private readonly System.Windows.Threading.DispatcherTimer _flow = new() { Interval = TimeSpan.FromMilliseconds(80) };
    private double _flowPhase;
    private bool? _heatConfirmed;
    private bool? _valveOpen;
    private bool _igniting;
    private bool _fanSpin;
    private bool _gasOk;

    public MainView()
    {
        InitializeComponent();
        ProcessText.Text = IntPtr.Size == 4
            ? "本进程是 32 位。顶层窗口类名是手写的 ControlPanelClass，WPF 只是里面的内容，不会自动拥有这个类名。"
            : "本进程不是 32 位，不能加载厂商 DLL。请用 x86 的 CtrlPanel.exe。";
        _settings = PanelSettings.Load(SettingsPath());
        HostBox.Text = _settings.Host;
        PortBox.Text = _settings.Port.ToString(System.Globalization.CultureInfo.InvariantCulture);
        DllBox.Text = _settings.DllPath;
        AcceptBox.IsChecked = _settings.RiskAcknowledged;
        ReloadEvents();
        StatusText.Text = _settings.StartupStatus();
        PlaceSchematic();
        ShowPage(0);
        ShowSettings(0);
        RefreshMainActions();
        _watch.Tick += (_, _) =>
        {
            ShowWatch();
            RefreshMainActions();
        };
        _flow.Tick += (_, _) => MoveGasDots();
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
        if (dialog.ShowDialog() != true)
        {
            return;
        }
        string problem = VendorDllIdentity.Describe(dialog.FileName);
        if (problem.Length != 0)
        {
            Note(problem);
            return;
        }
        string dest = VendorDllIdentity.FixedPath(AppContext.BaseDirectory);
        if (!string.Equals(Path.GetFullPath(dialog.FileName), Path.GetFullPath(dest), StringComparison.OrdinalIgnoreCase))
        {
            File.Copy(dialog.FileName, dest, true);
        }
        _settings.DllPath = dest;
        DllBox.Text = dest;
        _settings.Save(SettingsPath());
        Note("已核对并保存到程序目录的 HWSendData.dll。未改 HW 安装目录。");
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
        RefreshMainActions();
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
        _heatConfirmed = null;
        _valveOpen = null;
        _igniting = false;
        _fanSpin = false;
        if (run != null)
        {
            await run.CancelAsync();
            run.Dispose();
        }
        if (transport != null)
        {
            await transport.DisposeAsync();
        }
        _ = Dispatcher.BeginInvoke(RefreshMainActions);
    }

    private void OpenSettings_Click(object sender, RoutedEventArgs e)
    {
        ShowPage(5);
    }

    private void Nav_Click(object sender, RoutedEventArgs e)
    {
        if (sender is Button button && int.TryParse(button.Tag as string, out int index))
        {
            ShowPage(index);
        }
    }

    private void SettingTab_Click(object sender, RoutedEventArgs e)
    {
        if (sender is Button button && int.TryParse(button.Tag as string, out int index))
        {
            ShowSettings(index);
        }
    }

    private void ShowPage(int index)
    {
        Grid[] pages = { PageMain, PageOven, PageDetector, PageEvents, PageWatch, PageSettings };
        NavIcon[] icons = { NavIcon0, NavIcon1, NavIcon2, NavIcon3, NavIcon4, NavIcon5 };
        TextBlock[] labels = { NavLabel0, NavLabel1, NavLabel2, NavLabel3, NavLabel4, NavLabel5 };
        var blue = new SolidColorBrush(Color.FromRgb(0x2F, 0x6F, 0xEB));
        var muted = new SolidColorBrush(Color.FromRgb(0x7E, 0x8C, 0x9E));
        blue.Freeze();
        muted.Freeze();
        for (int i = 0; i < pages.Length; i++)
        {
            bool on = i == index;
            pages[i].Visibility = on ? Visibility.Visible : Visibility.Collapsed;
            icons[i].Active = on;
            labels[i].Foreground = on ? blue : muted;
            labels[i].FontWeight = on ? FontWeights.Bold : FontWeights.Normal;
        }
    }

    private void ShowSettings(int index)
    {
        StackPanel[] panes = { SettingsCalib, SettingsPid, SettingsParams, SettingsComm };
        Button[] tabs = { SettingTab0, SettingTab1, SettingTab2, SettingTab3 };
        var ink = new SolidColorBrush(Color.FromRgb(0x2A, 0x35, 0x42));
        var slate = new SolidColorBrush(Color.FromRgb(0x4E, 0x5C, 0x6B));
        var idle = new SolidColorBrush(Color.FromRgb(0xEC, 0xEF, 0xF2));
        ink.Freeze();
        slate.Freeze();
        idle.Freeze();
        for (int i = 0; i < panes.Length; i++)
        {
            bool on = i == index;
            panes[i].Visibility = on ? Visibility.Visible : Visibility.Collapsed;
            tabs[i].Foreground = on ? Brushes.White : slate;
            tabs[i].Background = on ? ink : idle;
        }
    }

    private void AnalysisToggle_Click(object sender, RoutedEventArgs e)
    {
        ActionFace face = PanelActions.Analysis(_link?.AnalysisPhase);
        if (face.Role == PanelActions.Stop)
        {
            StopAnalysis_Click(sender, e);
        }
        else if (face.Role == PanelActions.Start)
        {
            StartAnalysis_Click(sender, e);
        }
        RefreshMainActions();
    }

    private async void HeatToggle_Click(object sender, RoutedEventArgs e)
    {
        ActionFace face = PanelActions.Heat(_link != null, _heatConfirmed, _gasOk);
        if (face.Role == PanelActions.None)
        {
            Note("两路气压未在 3 秒内都 ≥ 0.1 MPa，未发送加热启动。");
            return;
        }
        if (_link == null)
        {
            Note("未连接。已发送的停止不会把温控记成关闭。");
            if (face.Role == PanelActions.Stop)
            {
                _heatConfirmed = null;
            }
            RefreshMainActions();
            return;
        }
        if (face.Role == PanelActions.Stop)
        {
            bool inlet = Accepted(await SendLine("STOP_INLET_HEAT", null));
            bool detector = Accepted(await SendLine("STOP_DETECTOR_HEAT", null));
            bool oven = Accepted(await SendLine("STOP_OVEN_CONTROL", null));
            _heatConfirmed = PanelActions.HeatAfterStop(inlet && detector && oven);
        }
        else if (face.Role == PanelActions.Start)
        {
            if (string.IsNullOrWhiteSpace(InletSet.Text) || string.IsNullOrWhiteSpace(DetSet.Text))
            {
                Note("加热设定是空的，未发送。");
                return;
            }
            string inletSet = InletSet.Text.Trim();
            string detectorSet = DetSet.Text.Trim();
            bool ok = Accepted(await SendLine("WRITE_ZONE_SELECT", "{\"sel\":\"7\"}"));
            ok = Accepted(await SendLine("WRITE_INLET_TEMP", "{\"setpoint\":\"" + inletSet + "\"}")) && ok;
            ok = Accepted(await SendLine("WRITE_DETECTOR_TEMP", "{\"setpoint\":\"" + detectorSet + "\"}")) && ok;
            ok = Accepted(await SendLine("START_INLET_HEAT", null)) && ok;
            ok = Accepted(await SendLine("START_DETECTOR_HEAT", null)) && ok;
            ok = Accepted(await SendLine("START_OVEN_CONTROL", null)) && ok;
            _heatConfirmed = PanelActions.HeatAfterStart(_heatConfirmed, _gasOk, ok);
        }
        RefreshMainActions();
    }

    private async void ValveToggle_Click(object sender, RoutedEventArgs e)
    {
        ActionFace face = PanelActions.Valve(_valveOpen);
        if (_link == null)
        {
            _valveOpen = null;
            Note("未连接，空气阀状态未知，未改记为开或关。");
            RefreshMainActions();
            return;
        }
        string reply = face.Role == PanelActions.ValveOpen
            ? await SendLine("START_FID_VALVES", null)
            : await SendLine("STOP_FID_VALVES", null);
        _valveOpen = PanelActions.ValveAfterReply(_valveOpen, reply, face.Role);
        RefreshMainActions();
    }

    private async void IgniteMain_Click(object sender, RoutedEventArgs e)
    {
        if (_igniting)
        {
            Note("点火进行中。协议没有单独的停止点火命令，关阀不会当作停止点火。");
            return;
        }
        string reply = await SendLine("START_FID_IGNITE", null);
        _igniting = PanelActions.IgnitionAccepted(reply);
        if (!_igniting)
        {
            Note("点火命令没有被接受，可以再试。没有发送停止点火。");
        }
        else
        {
            Note("点火已发出。没有停止点火命令。火焰以随后的状态读取为准，这不是已经着火。");
        }
        RefreshMainActions();
    }

    private static bool Accepted(string reply) => reply.Contains("WRITE_OK", StringComparison.Ordinal);

    private void RefreshMainActions()
    {
        ActionFace analysis = PanelActions.Analysis(_link?.AnalysisPhase);
        AnalysisButton.Content = analysis.Text;
        AnalysisButton.IsEnabled = analysis.Enabled;
        AnalysisButton.Background = Freeze(analysis.Role == PanelActions.Stop ? "#2F6FEB" : "#2F6FEB");
        AcqStatusText.Text = PanelActions.AnalysisStatus(_link?.AnalysisPhase);
        ActionFace heat = PanelActions.Heat(_link != null, _heatConfirmed, _gasOk);
        HeatButton.Content = heat.Text;
        HeatButton.IsEnabled = heat.Enabled;
        HeatButton.Background = Freeze(heat.Role == PanelActions.None ? "#7E8C9E" : heat.Role == PanelActions.Stop ? "#FF7018" : "#2F6FEB");
        HeatText.Text = PanelActions.HeatStatus(_link != null, _heatConfirmed);
        ActionFace valve = PanelActions.Valve(_valveOpen);
        ValveButton.Content = valve.Text;
        ValveButton.IsEnabled = valve.Enabled;
        ValveButton.Background = Freeze(_valveOpen == null ? "#7E8C9E" : _valveOpen == true ? "#FF7018" : "#14488C");
        ActionFace ignite = PanelActions.Ignition(_igniting);
        IgniteButton.Content = ignite.Text;
        IgniteButton.IsEnabled = ignite.Enabled;
        IgniteButton.Background = Freeze("#A32020");
    }

    private static System.Windows.Media.SolidColorBrush Freeze(string hex)
    {
        var brush = new System.Windows.Media.SolidColorBrush((System.Windows.Media.Color)System.Windows.Media.ColorConverter.ConvertFromString(hex));
        brush.Freeze();
        return brush;
    }

    private async void StartAnalysis_Click(object sender, RoutedEventArgs e)
    {
        RememberSettings();
        string home = _settings.EffectiveHome;
        string freqPath = Path.Combine(home, "HWFrequence.txt");
        bool exists = false;
        string? text = null;
        try
        {
            exists = File.Exists(freqPath);
            if (exists)
            {
                text = File.ReadAllText(freqPath);
            }
        }
        catch (Exception ex)
        {
            Note("读取频率文件失败，未修改它：" + ex.Message);
            return;
        }
        string dll = string.IsNullOrWhiteSpace(_settings.DllPath)
            ? VendorDllIdentity.FixedPath(AppContext.BaseDirectory)
            : _settings.DllPath;
        DailyPrepResult prep = DailyPrep.Evaluate(_settings, exists, text, dll);
        ConfigText.Text = prep.Detail;
        if (!prep.Ok)
        {
            Note(prep.Detail);
            return;
        }
        if (!_sink.IsReady)
        {
            string loaded = _sink.Load(prep.DllPath);
            if (loaded.Length != 0)
            {
                Note(loaded);
                return;
            }
        }
        _config = prep.Frequence;
        if (_link == null)
        {
            if (_run != null)
            {
                Note("正在连接，尚未开始分析。");
                return;
            }
            var cts = new CancellationTokenSource();
            lock (_life)
            {
                _run = cts;
            }
            await ConnectAsync(_settings.Host, _settings.Port, cts);
        }
        _link?.RequestStartAnalysis(prep.Frequence, true);
        Note("已按保存的地址请求开始分析。先等设备确认和第一个真实点，再通知 HW 取数。");
    }

    private void SaveSettings_Click(object sender, RoutedEventArgs e)
    {
        RememberSettings();
        Note("已保存主机、端口和 HW 目录。启动时不会连接，也不会改 HWFrequence.txt。");
    }

    private void RememberSettings()
    {
        _settings.Host = HostBox.Text.Trim();
        _settings.Port = int.TryParse(PortBox.Text.Trim(), out int port) ? port : 0;
        _settings.RiskAcknowledged = AcceptBox.IsChecked == true;
        _settings.DllPath = DllBox.Text.Trim();
        _settings.Save(SettingsPath());
    }

    private static string SettingsPath() => Path.Combine(AppContext.BaseDirectory, "panel.settings");

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

    private async void ReadTemps_Click(object sender, RoutedEventArgs e)
    {
        string oven = await SendLine("READ_OVEN_TEMP", null);
        string inlet = await SendLine("READ_INLET_TEMP", null);
        string detector = await SendLine("READ_DETECTOR_TEMP", null);
        string gas = await SendLine("READ_GAS_PRESSURE", null);
        string fid = await SendLine("READ_FID_STATUS", null);
        bool flame = JsonFields.TryNumber(fid, "flame", out double flameValue) && flameValue != 0;
        FlameImage.Visibility = flame ? Visibility.Visible : Visibility.Collapsed;
        InletCard.Text = "进样口\n设定 " + JsonFields.Show(inlet, "setpoint") + "\n实测 " + JsonFields.Show(inlet, "actual");
        DetCard.Text = "检测器\n设定 " + JsonFields.Show(detector, "setpoint") + "\n实测 " + JsonFields.Show(detector, "actual");
        OvenCard.Text = "柱箱\n设定 " + JsonFields.Show(oven, "setpoint") + "\n实测 " + JsonFields.Show(oven, "actual");
        if (JsonFields.TryNumber(oven, "fan", out double fan))
        {
            _fanSpin = fan != 0;
            FanCard.Text = _fanSpin ? "运转" : "停止";
            if (!_fanSpin)
            {
                FanRotate.Angle = 0;
            }
        }
        else
        {
            _fanSpin = false;
            FanRotate.Angle = 0;
            FanCard.Text = "未知";
        }
        if (JsonFields.TryNumber(oven, "vent", out double vent))
        {
            VentDoorRotate.Angle = vent != 0 ? -64 : 0;
            VentCard.Text = vent != 0 ? "开" : "关";
        }
        else
        {
            VentDoorRotate.Angle = 0;
            VentCard.Text = "未知";
        }
        ApplyFidRead(fid, fid.Contains("WRITE_FAIL", StringComparison.Ordinal) || fid.Contains("未连接", StringComparison.Ordinal) ? false : true);
        bool h2Ok = JsonFields.TryNumber(gas, "h2", out double h2) || JsonFields.TryNumber(gas, "adc1", out h2);
        bool airOk = JsonFields.TryNumber(gas, "air", out double air) || JsonFields.TryNumber(gas, "adc2", out air);
        long now = Environment.TickCount64;
        _link?.NoteGas(h2Ok ? h2 : null, airOk ? air : null, now);
        string? block = GasSupply.BlockReason(h2Ok ? h2 : null, airOk ? air : null, now, now);
        _gasOk = block == null;
        bool flow = _gasOk;
        GasDotA.Visibility = flow ? Visibility.Visible : Visibility.Collapsed;
        GasDotB.Visibility = flow ? Visibility.Visible : Visibility.Collapsed;
        if (flow)
        {
            _flow.Start();
        }
        else
        {
            _flow.Stop();
        }
        bool igniting = fid.Contains("igniting", StringComparison.OrdinalIgnoreCase);
        FilamentImage.Visibility = igniting ? Visibility.Visible : Visibility.Collapsed;
        ZoneText.Text = (block == null ? "气路动画开。 " : "气路动画停。 ")
            + "氢气 " + (h2Ok ? h2.ToString(System.Globalization.CultureInfo.InvariantCulture) : "未读取到")
            + " MPa；空气 " + (airOk ? air.ToString(System.Globalization.CultureInfo.InvariantCulture) : "未读取到") + " MPa";
        if (JsonFields.Has(oven, "fault") && JsonFields.TryNumber(oven, "fault", out double fault) && fault != 0)
        {
            EventLog.Report(EventPath(), "oven-fault", "柱箱故障", "READ_OVEN_TEMP fault=" + fault.ToString(System.Globalization.CultureInfo.InvariantCulture));
        }
        if (JsonFields.Has(inlet, "valid") && JsonFields.TryNumber(inlet, "valid", out double valid) && valid == 0)
        {
            EventLog.Report(EventPath(), "inlet-sensor", "进样口温度无效", "READ_INLET_TEMP valid=0。这是记录，没有另发命令。");
        }
        ReloadEvents();
        RefreshMainActions();
    }

    private void ApplyFidRead(string fid, bool readReachedDevice)
    {
        _valveOpen = PanelActions.ValveAfterRead(_valveOpen, fid, readReachedDevice);
        if (!fid.Contains("igniting", StringComparison.OrdinalIgnoreCase))
        {
            _igniting = false;
        }
        bool flame = JsonFields.TryNumber(fid, "flame", out double flameValue) && flameValue != 0;
        FlameImage.Visibility = flame ? Visibility.Visible : Visibility.Collapsed;
        FilamentImage.Visibility = _igniting && !flame ? Visibility.Visible : Visibility.Collapsed;
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
        _gasOk = block == null;
        ApplyFidRead(fid, !fid.Contains("未连接", StringComparison.Ordinal));
        DetText.Text = "氢气 " + (h2Ok ? h2.ToString(System.Globalization.CultureInfo.InvariantCulture) : "未读取到")
            + " MPa；空气 " + (airOk ? air.ToString(System.Globalization.CultureInfo.InvariantCulture) : "未读取到")
            + " MPa；火焰 " + JsonFields.Show(fid, "flame") + "；电流 pA " + JsonFields.Show(fid, "currentPa")
            + "。点火应答不代表已经着火。";
        RefreshMainActions();
    }

    private async void Ignite_Click(object sender, RoutedEventArgs e)
    {
        string reply = await SendLine("START_FID_IGNITE", null);
        _igniting = PanelActions.IgnitionAccepted(reply);
        DetText.Text = reply + " 火焰以随后的状态读取为准。没有停止点火命令。";
        RefreshMainActions();
    }

    private async void ValveOn_Click(object sender, RoutedEventArgs e)
    {
        string reply = await SendLine("START_FID_VALVES", null);
        _valveOpen = PanelActions.ValveAfterReply(_valveOpen, reply, PanelActions.ValveOpen);
        DetText.Text = reply;
        RefreshMainActions();
    }

    private async void ValveOff_Click(object sender, RoutedEventArgs e)
    {
        string reply = await SendLine("STOP_FID_VALVES", null);
        _valveOpen = PanelActions.ValveAfterReply(_valveOpen, reply, PanelActions.ValveClose);
        DetText.Text = reply;
        RefreshMainActions();
    }
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
        PidBox.Text = await ReadPidLine("READ_OVEN_PID");
        InletPidBox.Text = await ReadPidLine("READ_INLET_PID");
        DetPidBox.Text = await ReadPidLine("READ_DETECTOR_PID");
    }

    private async Task<string> ReadPidLine(string verb)
    {
        string json = await SendLine(verb, null);
        return JsonFields.Show(json, "kp") + " " + JsonFields.Show(json, "ki") + " " + JsonFields.Show(json, "kd");
    }

    private async void WritePid_Click(object sender, RoutedEventArgs e)
    {
        if (!await WritePidLine("WRITE_OVEN_PID", PidBox.Text))
        {
            return;
        }
        if (!await WritePidLine("WRITE_INLET_PID", InletPidBox.Text))
        {
            return;
        }
        await WritePidLine("WRITE_DETECTOR_PID", DetPidBox.Text);
    }

    private async Task<bool> WritePidLine(string verb, string text)
    {
        string[] part = text.Split(' ', StringSplitOptions.RemoveEmptyEntries);
        if (part.Length != 3 || part.Any(item => item == "未读取到"))
        {
            Note(verb + " 不是三个已读取的数，未发送。");
            return false;
        }
        string reply = await SendLine(verb, "{\"kp\":\"" + part[0] + "\",\"ki\":\"" + part[1] + "\",\"kd\":\"" + part[2] + "\"}");
        return reply.Contains("WRITE_OK", StringComparison.Ordinal);
    }

    private void ClearEvents_Click(object sender, RoutedEventArgs e)
    {
        EventLog.Clear(EventPath());
        ReloadEvents();
    }

    private void ReloadEvents()
    {
        Events.Items.Clear();
        foreach (PanelEvent item in EventLog.Load(EventPath()))
        {
            Events.Items.Add(item.Time + "  " + item.Name + "  " + item.Detail);
        }
    }

    private static string EventPath() => Path.Combine(AppContext.BaseDirectory, "panel.events");

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

    private void PlaceSchematic()
    {
        SchematicCanvas.Width = SchematicLayout.Width;
        SchematicCanvas.Height = SchematicLayout.Height;
        Put(FidImage, SchematicLayout.Fid);
        Put(InletImage, SchematicLayout.Inlet);
        Put(ColumnImage, SchematicLayout.Column);
        Put(DetBorder, SchematicLayout.DetectorCard);
        Put(InletBorder, SchematicLayout.InletCard);
        Put(OvenBorder, SchematicLayout.OvenCard);
        Put(FanBorder, SchematicLayout.FanCard);
        Put(VentBorder, SchematicLayout.VentCard);
        SchematicLayout.Pt glyph = SchematicLayout.MapUniform(SchematicLayout.Column, SchematicLayout.ColumnPixelsW, SchematicLayout.ColumnPixelsH, 0, SchematicLayout.ColumnGlyphTop);
        ColumnImage.Clip = new System.Windows.Media.RectangleGeometry(new Rect(0, 0, SchematicLayout.Column.W, Math.Max(1, glyph.Y - SchematicLayout.Column.Y)));
        Canvas.SetLeft(ColumnCaption, SchematicLayout.Column.X + SchematicLayout.Column.W / 2 - 20);
        Canvas.SetTop(ColumnCaption, SchematicLayout.Column.Y + SchematicLayout.Column.H * 0.956 - 9);
        var flame = SchematicLayout.MapUniform(SchematicLayout.Fid, SchematicLayout.FidPixelsW, SchematicLayout.FidPixelsH, 287, 150);
        Canvas.SetLeft(FlameImage, flame.X - 16);
        Canvas.SetTop(FlameImage, flame.Y - 16);
        FlameImage.Width = 32;
        FlameImage.Height = 32;
        var filament = SchematicLayout.MapUniform(SchematicLayout.Fid, SchematicLayout.FidPixelsW, SchematicLayout.FidPixelsH, 150, 200);
        Canvas.SetLeft(FilamentImage, filament.X - 14);
        Canvas.SetTop(FilamentImage, filament.Y - 10);
        FilamentImage.Width = 28;
        FilamentImage.Height = 20;
        LeftPipe.Data = Pipe(SchematicLayout.LeftPipe);
        RightPipe.Data = Pipe(SchematicLayout.RightPipe);
        Panel.SetZIndex(LeftPipe, 2);
        Panel.SetZIndex(RightPipe, 2);
    }

    private static void Put(FrameworkElement element, SchematicLayout.Box box)
    {
        Canvas.SetLeft(element, box.X);
        Canvas.SetTop(element, box.Y);
        element.Width = box.W;
        element.Height = box.H;
    }

    private static System.Windows.Media.Geometry Pipe(SchematicLayout.Pt[] points)
    {
        var figure = new System.Windows.Media.PathFigure { StartPoint = new System.Windows.Point(points[0].X, points[0].Y) };
        figure.Segments.Add(new System.Windows.Media.LineSegment(new System.Windows.Point(points[1].X, points[1].Y), true));
        figure.Segments.Add(new System.Windows.Media.LineSegment(new System.Windows.Point(points[2].X, points[2].Y), true));
        figure.Segments.Add(new System.Windows.Media.LineSegment(new System.Windows.Point(points[3].X, points[3].Y), true));
        return new System.Windows.Media.PathGeometry(new[] { figure });
    }

    private void MoveGasDots()
    {
        _flowPhase += 0.04;
        if (_flowPhase > 1)
        {
            _flowPhase -= 1;
        }
        PlaceAlong(GasDotA, SchematicLayout.LeftPipe, _flowPhase);
        PlaceAlong(GasDotB, SchematicLayout.RightPipe, _flowPhase);
        if (_fanSpin)
        {
            FanRotate.Angle = (FanRotate.Angle + 12) % 360;
        }
    }

    private static void PlaceAlong(System.Windows.Shapes.Ellipse dot, SchematicLayout.Pt[] points, double t)
    {
        double total = 0;
        for (int i = 1; i < points.Length; i++)
        {
            total += Distance(points[i - 1].X, points[i - 1].Y, points[i].X, points[i].Y);
        }
        double remain = total * t;
        for (int i = 1; i < points.Length; i++)
        {
            double span = Distance(points[i - 1].X, points[i - 1].Y, points[i].X, points[i].Y);
            if (remain <= span || i == points.Length - 1)
            {
                double f = span <= 0 ? 0 : remain / span;
                Canvas.SetLeft(dot, points[i - 1].X + (points[i].X - points[i - 1].X) * f - 3);
                Canvas.SetTop(dot, points[i - 1].Y + (points[i].Y - points[i - 1].Y) * f - 3);
                return;
            }
            remain -= span;
        }
    }

    private static double Distance(double x0, double y0, double x1, double y1)
    {
        double dx = x0 - x1;
        double dy = y0 - y1;
        return Math.Sqrt(dx * dx + dy * dy);
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
    }
}
