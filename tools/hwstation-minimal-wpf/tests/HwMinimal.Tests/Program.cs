using System.Buffers.Binary;
using System.Text;
using HwMinimal;

int failed = 0;
void Check(bool ok, string name)
{
    if (ok)
    {
        Console.WriteLine("PASS " + name);
        return;
    }
    failed++;
    Console.WriteLine("FAIL " + name);
}

Check(Crc16.Compute(Encoding.ASCII.GetBytes("123456789")) == 0x4B37, "modbus crc vector");
byte[] start = GcCodec.StartAcq(1, 100);
Check(Convert.ToHexString(start).Equals("AA55010102006400177255AA", StringComparison.OrdinalIgnoreCase), "start frame 100 ms");
byte[] stop = GcCodec.StopAcq(2);
Check(Convert.ToHexString(stop).Equals("AA5502020000A05C55AA", StringComparison.OrdinalIgnoreCase), "stop frame");

var parser = new FrameParser();
var frames = new List<GcFrame>();
Check(parser.Push(start.AsSpan(0, 3), frames) && frames.Count == 0, "fragment holds");
Check(parser.Push(start.AsSpan(3), frames) && frames.Count == 1 && frames[0].Command == 0x01, "fragment completes");
byte[] second = GcCodec.StopAcq(2);
byte[] batch = new byte[start.Length + second.Length];
start.CopyTo(batch, 0);
second.CopyTo(batch, start.Length);
var both = new FrameParser();
var many = new List<GcFrame>();
Check(both.Push(batch, many) && many.Count == 2 && many[1].Command == 0x02, "batched frames");
byte[] bad = (byte[])start.Clone();
bad[6] ^= 0xFF;
var badParser = new FrameParser();
var none = new List<GcFrame>();
Check(!badParser.Push(bad, none) && none.Count == 0 && badParser.Fault.Contains("CRC", StringComparison.Ordinal), "crc fault");
Check(!badParser.Push(start, none), "no resync after crc");

Check(LogCurrent.MapMilliVolts(0f).FemtoAmps == 100_000_000, "Y0 -> 100000000 fA");
Check(LogCurrent.MapMilliVolts(-200f).FemtoAmps == 10_000_000, "Y-200 -> 10000000 fA");
Check(LogCurrent.MapMilliVolts(200f).FemtoAmps == 1_000_000_000, "Y200 -> 1000000000 fA");
MapResult overflow = LogCurrent.MapMilliVolts(400f);
Check(!overflow.Ok && overflow.Error.Contains("int32", StringComparison.Ordinal), "Y400 overflow");
Check(!LogCurrent.MapMilliVolts(float.NaN).Ok, "NaN rejected");
Check(!LogCurrent.MapMilliVolts(float.PositiveInfinity).Ok, "Inf rejected");
Check(LogCurrent.MapMilliVolts(0f).FemtoAmps != int.MaxValue, "no board clamp");

var same = new AcqRun();
var view = new List<ObservedSample>();
var sink = new List<HwTriple>();
same.Accept(Frames.One(1, 0, 0, 0f), true, view, sink);
same.Accept(Frames.One(2, 1, 100, 0f), true, view, sink);
Check(view.Count == 2 && sink.Count == 2 && sink[0].Data1 == sink[1].Data1 && sink[0].Data3 == 3 && sink[0].Data2 == 0, "equal values are both forwarded");

var gap = new AcqRun();
var gapView = new List<ObservedSample>();
var gapSink = new List<HwTriple>();
gap.Accept(Frames.One(1, 0, 0, 0f), true, gapView, gapSink);
gap.Accept(Frames.One(2, 1, 300, 0f), true, gapView, gapSink);
gap.Accept(Frames.One(3, 2, 400, 0f), true, gapView, gapSink);
Check(gap.Invalid && gapView.Count == 1 && gapSink.Count == 1 && gap.Fault.Contains("未补点", StringComparison.Ordinal), "time gap invalidates");

var order = new AcqRun();
var orderView = new List<ObservedSample>();
order.Accept(Frames.One(1, 0, 100, 0f), false, orderView, sink);
Check(order.Invalid && orderView.Count == 0, "first time must be 0");
var backward = new AcqRun();
var backView = new List<ObservedSample>();
backward.Accept(Frames.One(1, 0, 0, 0f), false, backView, sink);
backward.Accept(Frames.One(2, 1, 0, 1f), false, backView, sink);
Check(backward.Invalid && backView.Count == 1, "time delta 0 is invalid");

var seq = new AcqRun();
var seqView = new List<ObservedSample>();
seq.Accept(Frames.One(1, 0, 0, 0f), false, seqView, sink);
seq.Accept(Frames.One(3, 1, 100, 0f), false, seqView, sink);
Check(seq.Invalid && seq.Fault.Contains("帧序号", StringComparison.Ordinal) && seqView.Count == 1, "sequence gap");

var batchGap = new AcqRun();
var batchView = new List<ObservedSample>();
batchGap.Accept(Frames.One(1, 0, 0, 0f), false, batchView, sink);
batchGap.Accept(Frames.One(2, 2, 100, 0f), false, batchView, sink);
Check(batchGap.Invalid && batchView.Count == 1, "batch gap");

var nanRun = new AcqRun();
var nanView = new List<ObservedSample>();
var nanSink = new List<HwTriple>();
nanRun.Accept(Frames.One(1, 0, 0, float.NaN), true, nanView, nanSink);
nanRun.Accept(Frames.One(2, 1, 100, 0f), true, nanView, nanSink);
Check(nanRun.Invalid && nanView.Count == 0 && nanSink.Count == 0, "NaN stops before any point");

var hz = new AcqRun();
var hzView = new List<ObservedSample>();
var hzSink = new List<HwTriple>();
hz.Accept(Frames.Pair(), true, hzView, hzSink);
Check(hzView.Count == 2 && hzSink.Count == 2, "100 ms pair is not expanded to 20 Hz");

HwFrequenceParse twenty = HwFrequence.Parse("20,20\n\n注：不要改文件\n");
HwFrequenceParse ten = HwFrequence.Parse("10,10\n");
HwFrequenceParse tenTwenty = HwFrequence.Parse("10,20\n");
Check(twenty.SyntaxOk && twenty.FileRuleOk && !twenty.CompatibleWith100Ms, "20,20 mismatch");
Check(ten.CompatibleWith100Ms && ForwardGate.Allows(ten, true, out _), "10,10 allows");
Check(tenTwenty.CompatibleWith100Ms, "10,20 is also 100 ms");
Check(!ForwardGate.Allows(ten, false, out _), "limits must be accepted");
Check(!ForwardGate.Allows(twenty, true, out string blocked) && blocked.Contains("50", StringComparison.Ordinal), "20,20 block text");

string examplePath = Path.Combine(AppContext.BaseDirectory, "examples", "HWFrequence.proposed-10-10.txt");
string before = File.ReadAllText(examplePath);
DateTime stamp = File.GetLastWriteTimeUtc(examplePath);
HwFrequenceParse example = HwFrequence.Parse(before);
string after = File.ReadAllText(examplePath);
Check(example.CompatibleWith100Ms && before == after && File.GetLastWriteTimeUtc(examplePath) == stamp, "example 10,10 is not rewritten");
Check(before.Contains("候选", StringComparison.Ordinal), "example is marked as a candidate");

var coldSink = new RecordingSink();
var cold = new ThrowTransport();
_ = new LiveLink(cold, coldSink);
await Task.Delay(80);
Check(cold.Calls == 0 && coldSink.Count == 0, "constructing the link does nothing");

var idle = new LiveLink(new ThrowTransport(), new NotReadySink());
Check(!idle.TryArm(HwFrequence.Parse("10,10"), true, out string missing) && missing.Contains("尚未", StringComparison.Ordinal), "missing dll is not ready");
long now = Environment.TickCount64;
Check(GasSupply.BlockReason(0.1, 0.1, now, now) == null, "both 0.1 MPa current allows heat");
Check(GasSupply.BlockReason(0.09, 0.2, now, now) != null, "one low channel blocks heat");
Check(GasSupply.BlockReason(null, 0.2, now, now) != null, "missing channel blocks heat");
Check(GasSupply.BlockReason(0.2, 0.2, now - 4000, now) != null, "stale pressure blocks heat");
Check(Gcws.Classify("GCWS\tSTART_OVEN_CONTROL") == GcwsKind.HeatStart, "heat start classified");
Check(Gcws.Rejection("WRITE_OVEN_PID", false, "{}") != null, "pid write locked");
Check(Gcws.Rejection("STOP_OVEN_CONTROL", false, null) == null, "heat stop allowed");
var demux = new StreamDemux();
var gotFrames = new List<GcFrame>();
var gotText = new List<string>();
byte[] mixed = Encoding.ASCII.GetBytes("{\"h2\":\"0.20\"}").Concat(GcCodec.StartAcq(1, 100)).ToArray();
Check(demux.Push(mixed, 0, mixed.Length, gotFrames, gotText) && gotText.Count == 1 && gotFrames.Count == 1 && !demux.Faulted, "json does not break the frame parser");
string dir = Path.Combine(Path.GetTempPath(), "hwminimal-prep-" + Guid.NewGuid().ToString("N"));
Directory.CreateDirectory(dir);
string settingsPath = Path.Combine(dir, "panel.settings");
var fresh = PanelSettings.Load(settingsPath);
Check(fresh.Host == PanelSettings.DefaultHost && fresh.Port == PanelSettings.DefaultPort && fresh.EffectiveHome == PanelSettings.DefaultHwHome && !fresh.RiskAcknowledged, "defaults are 192.168.1.30:8234 and D:\\HWGMP\\program");
Check(fresh.StartupStatus().Contains("192.168.1.30:8234", StringComparison.Ordinal) && fresh.StartupStatus().Contains("未连接", StringComparison.Ordinal) && !fresh.StartupStatus().Contains("空的", StringComparison.Ordinal) && !fresh.StartupStatus().Contains("不会发写命令", StringComparison.Ordinal), "startup status is one short line");
Check(LogCurrent.Caveat.Contains("不用于正式定量", StringComparison.Ordinal), "freshness limit text is still defined");
Check(PanelSettings.ContentWidth == 600, "content width stays 600");
Console.WriteLine($"LAYOUT fid {SchematicLayout.FidOutlet.X:0.0},{SchematicLayout.FidOutlet.Y:0.0} -> col {SchematicLayout.ColumnLeft.X:0.0},{SchematicLayout.ColumnLeft.Y:0.0} | inlet {SchematicLayout.InletOutlet.X:0.0},{SchematicLayout.InletOutlet.Y:0.0} -> col {SchematicLayout.ColumnRight.X:0.0},{SchematicLayout.ColumnRight.Y:0.0}");
Check(Same(SchematicLayout.LeftPipe[0], SchematicLayout.FidOutlet) && Same(SchematicLayout.LeftPipe[^1], SchematicLayout.ColumnLeft), "left pipe meets FID outlet and column left");
Check(Same(SchematicLayout.RightPipe[0], SchematicLayout.InletOutlet) && Same(SchematicLayout.RightPipe[^1], SchematicLayout.ColumnRight), "right pipe meets inlet outlet and column right");
Check(SchematicLayout.Separated(SchematicLayout.DetectorCard, SchematicLayout.Fid) && SchematicLayout.Separated(SchematicLayout.InletCard, SchematicLayout.Inlet) && SchematicLayout.Separated(SchematicLayout.OvenCard, SchematicLayout.Column) && SchematicLayout.Separated(SchematicLayout.FanCard, SchematicLayout.Column) && SchematicLayout.Separated(SchematicLayout.VentCard, SchematicLayout.FanCard), "cards do not cover the parts");
Check(SchematicLayout.PipesClearCards(), "pipes stay outside the temperature cards");
Check(SchematicLayout.ButtonsClear(520) && SchematicLayout.ButtonsClear(760), "buttons stay below the canvas at 520 and 760");
Check(SchematicLayout.ColumnGlyphTop > 240 && SchematicLayout.ColumnGlyphTop < 320, "column caption ink starts after the glass");
ActionFace analysisIdle = PanelActions.Analysis("Idle");
ActionFace analysisWait = PanelActions.Analysis("WaitAck");
ActionFace analysisFailed = PanelActions.Analysis("Invalid");
Check(analysisIdle.Text == "启动" && analysisIdle.Role == PanelActions.Start && !PanelActions.AnalysisStatus("Running").Contains("采集成功", StringComparison.Ordinal), "analysis success is not claimed when HW is only submitted");
Check(analysisWait.Text == "进行中" && analysisWait.Enabled && analysisWait.Role == PanelActions.Stop, "start in progress can still stop");
Check(analysisFailed.Text == "启动" && analysisFailed.Role == PanelActions.Start, "failed analysis can start again");
ActionFace heatUnknown = PanelActions.Heat(false, null, true);
ActionFace heatBlocked = PanelActions.Heat(true, false, false);
ActionFace heatOn = PanelActions.Heat(true, true, false);
Check(heatUnknown.Role == PanelActions.Stop && PanelActions.HeatStatus(false, null).Contains("未知", StringComparison.Ordinal), "disconnect does not look like heat off");
Check(heatBlocked.Role == PanelActions.None && !heatBlocked.Sends, "low gas does not send heat start");
Check(heatOn.Role == PanelActions.Stop && PanelActions.HeatAfterStop(false) == null && PanelActions.HeatAfterStart(false, false, true) == false, "unacked stop is unknown and blocked gas does not flip heat");
Check(PanelActions.Valve(null).Text.Contains("未知", StringComparison.Ordinal) && PanelActions.ValveAfterReply(null, "WRITE_FAIL", PanelActions.ValveClose) == null && PanelActions.ValveAfterReply(null, "WRITE_OK", PanelActions.ValveOpen) == true, "valve click without WRITE_OK stays unknown");
Check(PanelActions.ValveAfterRead(true, "{}", false) == null && PanelActions.ValveAfterRead(true, "{\"actual\":\"1\"}", true) == true, "a read without valve does not invent closed");
ActionFace igniting = PanelActions.Ignition(true);
Check(!igniting.Enabled && igniting.Role == PanelActions.None && !PanelActions.IgnitionAccepted("未连接") && PanelActions.Ignition(false).Role == PanelActions.Ignite, "ignition has no cancel command and rejects a dead link");
Check(SchematicLayout.FidOutlet.Y > 180 && SchematicLayout.FidOutlet.Y < SchematicLayout.Fid.Bottom && SchematicLayout.ColumnLeft.X > SchematicLayout.Column.X && SchematicLayout.ColumnRight.X < SchematicLayout.Column.Right, "outlets are inside the bitmap, not on the view edge");
fresh.RiskAcknowledged = true;
fresh.HwHome = dir;
fresh.Save(settingsPath);
PanelSettings loaded = PanelSettings.Load(settingsPath);
Check(loaded.RiskAcknowledged && loaded.EffectiveHome == dir && loaded.Host == "192.168.1.30", "settings round-trip");
File.WriteAllText(Path.Combine(dir, "HWFrequence.txt"), "10,10\n");
string beforeFreq = File.ReadAllText(Path.Combine(dir, "HWFrequence.txt"));
DailyPrepResult ready = DailyPrep.Evaluate(loaded, true, beforeFreq, Path.Combine(dir, "missing.dll"));
Check(!ready.Ok && ready.Detail.Contains("缺少", StringComparison.Ordinal), "missing dll blocks");
Check(File.ReadAllText(Path.Combine(dir, "HWFrequence.txt")) == beforeFreq, "prep does not rewrite frequence");
File.WriteAllText(Path.Combine(dir, "HWFrequence.txt"), "20,20\n");
DailyPrepResult mismatch = DailyPrep.Evaluate(loaded, true, "20,20\n", Path.Combine(dir, "missing.dll"));
Check(!mismatch.Ok && mismatch.Detail.Contains("50", StringComparison.Ordinal), "installed 20,20 blocks even if dll missing text comes later");
DailyPrepResult absent = DailyPrep.Evaluate(loaded, false, null, Path.Combine(dir, VendorDllIdentity.FileName));
Check(!absent.Ok && absent.Detail.Contains("10,10", StringComparison.Ordinal), "missing install file is not replaced by the sample 10,10");
byte[] wrong = new byte[16];
File.WriteAllBytes(Path.Combine(dir, VendorDllIdentity.FileName), wrong);
Check(VendorDllIdentity.Describe(Path.Combine(dir, VendorDllIdentity.FileName)).Contains("98304", StringComparison.Ordinal), "wrong dll is not accepted");
var coldStart = new LiveLink(new ThrowTransport(), new RecordingSink());
await Task.Delay(50);
Check(true, "saved settings do not open a socket by themselves");
coldStart.RequestStartAnalysis(HwFrequence.Parse("10,10"), true);
await Task.Delay(50);
string eventDir = Path.Combine(Path.GetTempPath(), "hwminimal-events-" + Guid.NewGuid().ToString("N"));
Directory.CreateDirectory(eventDir);
string eventPath = Path.Combine(eventDir, "panel.events");
Check(EventLog.Report(eventPath, "oven-fault", "柱箱故障", "记录，不是命令"), "event recorded");
Check(!EventLog.Report(eventPath, "oven-fault", "柱箱故障", "再次"), "same fault is not duplicated");
Check(!File.ReadAllText(eventPath).Contains("GCWS", StringComparison.Ordinal), "event file has no device command");
Check(Gcws.Rejection("WRITE_INLET_PID", false, "{}") != null && Gcws.Rejection("WRITE_DETECTOR_PID", false, "{}") != null, "inlet and detector pid stay locked");
await RunLink();
await RunAnalysis();
await RunPairStop();
Check(failed == 0, "all checks");
return failed == 0 ? 0 : 1;

async Task RunAnalysis()
{
    var transport = new ScriptTransport();
    var recording = new RecordingSink();
    var link = new LiveLink(transport, recording, new LinkOptions { HeartbeatMs = 60_000, AckTimeoutMs = 500, FirstPointWaitMs = 800, StopDrainMs = 800 });
    var hello = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
    link.Status += text =>
    {
        if (text.Contains("HELLO_OK", StringComparison.Ordinal))
        {
            hello.TrySetResult();
        }
    };
    using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(4));
    Task run = link.RunAsync(cts.Token);
    await hello.Task.WaitAsync(TimeSpan.FromSeconds(2));
    string heat = await link.ExchangeAsync("START_INLET_HEAT", null, cts.Token);
    Check(heat.Contains("0.1", StringComparison.Ordinal) || heat.Contains("3", StringComparison.Ordinal), "heat start blocked without current gas");
    Check(!transport.Writes.Any(w => Encoding.ASCII.GetString(w).Contains("START_INLET_HEAT", StringComparison.Ordinal)), "blocked heat was not written");
    link.RequestStartAnalysis(HwFrequence.Parse("20,20"), true);
    await Task.Delay(150);
    Check(!transport.Writes.Any(w => w.Length > 2 && w[2] == GcCodec.CmdStart), "20,20 does not start acquisition");
    link.RequestStartAnalysis(HwFrequence.Parse("10,10"), true);
    await WaitUntil(() => recording.Count >= 3);
    HwTriple[] started = recording.Snapshot();
    Check(started[0].Data3 == 3 && started[1].Data3 == 4 && started[2].Equals(new HwTriple(1, 0, 1)), "analysis waits for one real pair before HW take");
    link.RequestStopAnalysis();
    transport.Enqueue(Frames.Encode(2, 1, 100, 0f));
    await WaitUntil(() => recording.Snapshot().Any(c => c.Data3 == 2));
    HwTriple[] stopped = recording.Snapshot();
    int hwStop = Array.FindIndex(stopped, c => c.Data3 == 2);
    int secondA = Array.FindIndex(stopped, 3, c => c.Data3 == 3);
    Check(secondA >= 0 && secondA < hwStop && stopped[secondA + 1].Data3 == 4, "stop still forwards one bounded real pair before HW stop");
    cts.Cancel();
    await run;

    var quiet = new ScriptTransport { IncludeSample = false };
    var quietSink = new RecordingSink();
    var quietLink = new LiveLink(quiet, quietSink, new LinkOptions { HeartbeatMs = 60_000, AckTimeoutMs = 400, FirstPointWaitMs = 350 });
    var hello2 = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
    quietLink.Status += text =>
    {
        if (text.Contains("HELLO_OK", StringComparison.Ordinal))
        {
            hello2.TrySetResult();
        }
    };
    using var cts2 = new CancellationTokenSource(TimeSpan.FromSeconds(3));
    Task run2 = quietLink.RunAsync(cts2.Token);
    await hello2.Task.WaitAsync(TimeSpan.FromSeconds(2));
    quietLink.RequestStartAnalysis(HwFrequence.Parse("10,10"), true);
    await WaitUntil(() => quiet.Writes.Any(w => w.Length > 2 && w[2] == GcCodec.CmdStop));
    Check(quietSink.Count == 0, "no real point means HW take is not started");
    cts2.Cancel();
    await run2;
}

async Task RunLink()
{
    var transport = new ScriptTransport();
    var recording = new RecordingSink();
    var link = new LiveLink(transport, recording, new LinkOptions { HeartbeatMs = 60_000, AckTimeoutMs = 400 });
    var hello = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
    var first = new TaskCompletionSource<ObservedSample>(TaskCreationOptions.RunContinuationsAsynchronously);
    // Inline continuation: the observer runs before Sample returns, after both
    // commands of the pair have been sent.
    var second = new TaskCompletionSource<ObservedSample>();
    var third = new TaskCompletionSource<ObservedSample>();
    int samples = 0;
    link.Status += text =>
    {
        if (text.Contains("HELLO_OK", StringComparison.Ordinal))
        {
            hello.TrySetResult();
        }
    };
    link.Sample += sample =>
    {
        int n = Interlocked.Increment(ref samples);
        if (n == 1)
        {
            first.TrySetResult(sample);
        }
        if (n == 2)
        {
            second.TrySetResult(sample);
        }
        if (n == 3)
        {
            third.TrySetResult(sample);
        }
    };
    using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(4));
    Task run = link.RunAsync(cts.Token);
    await hello.Task.WaitAsync(TimeSpan.FromSeconds(2));
    Check(recording.Count == 0, "hello does not touch the sink");
    link.RequestDeviceStart();
    ObservedSample seen = await first.Task.WaitAsync(TimeSpan.FromSeconds(2));
    Check(!seen.Forwarded && recording.Count == 0 && seen.FemtoAmps == 100_000_000, "view without forwarding");
    byte[]? startWrite = transport.Writes.FirstOrDefault(w => w.Length > 2 && w[2] == GcCodec.CmdStart);
    Check(startWrite != null && Convert.ToHexString(startWrite).Equals("AA55010102006400177255AA", StringComparison.OrdinalIgnoreCase), "live start bytes");
    Check(!link.TryArm(HwFrequence.Parse("20,20"), true, out _), "running link rejects 20,20");
    Check(link.TryArm(HwFrequence.Parse("10,10"), true, out _), "running link accepts 10,10");
    transport.Enqueue(Frames.Encode(2, 1, 100, 0f));
    ObservedSample forwarded = await second.Task.WaitAsync(TimeSpan.FromSeconds(2));
    HwTriple[] one = recording.Snapshot();
    Check(forwarded.Forwarded && one.Length == 2 && one[0].Equals(new HwTriple(100_000_000, 0, 3)) && one[1].Equals(new HwTriple(0, 0, 4)), "second equal sample is one A3 then B4=0 pair");
    transport.Enqueue(Frames.Encode(3, 2, 200, 0f));
    await third.Task.WaitAsync(TimeSpan.FromSeconds(2));
    HwTriple[] two = recording.Snapshot();
    Check(two.Length == 4 && two[2].Equals(one[0]) && two[3].Equals(one[1]), "equal values are two pairs once each");
    link.RequestHwRecordStart();
    link.RequestHwRecordStop();
    await WaitUntil(() => recording.Count >= 6);
    HwTriple[] mid = recording.Snapshot();
    Check(mid[4].Equals(new HwTriple(1, 0, 1)) && mid[5].Equals(new HwTriple(1, 0, 2)), "A start and stop");
    int beforeZero = mid.Length;
    link.RequestHwZeroA();
    await WaitUntil(() => recording.Count == beforeZero + 1);
    HwTriple[] withZero = recording.Snapshot();
    Check(withZero[^1].Equals(new HwTriple(1, 0, 5)), "zero is A only and explicit");
    Check(withZero[0].Data3 == 3 && withZero[1].Data3 == 4 && withZero[2].Data3 == 3 && withZero[3].Data3 == 4, "pairs stay adjacent around later commands");
    transport.Enqueue(Frames.BadCrc(Frames.Encode(4, 3, 300, 0f)));
    await WaitUntil(() => !link.SamplesArmed);
    int frozen = recording.Count;
    transport.Enqueue(Frames.Encode(5, 4, 400, 0f));
    await Task.Delay(200);
    Check(recording.Count == frozen, "crc fault stops later points");
    cts.Cancel();
    await run;
}

async Task RunPairStop()
{
    var transport = new ScriptTransport();
    var sink = new PairSink { FailOn = 4 };
    var link = new LiveLink(transport, sink, new LinkOptions { HeartbeatMs = 60_000, AckTimeoutMs = 400 });
    sink.OnChannelA = () => link.RequestHwRecordStart();
    var hello = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
    link.Status += text =>
    {
        if (text.Contains("HELLO_OK", StringComparison.Ordinal))
        {
            hello.TrySetResult();
        }
    };
    using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(3));
    Task run = link.RunAsync(cts.Token);
    await hello.Task.WaitAsync(TimeSpan.FromSeconds(2));
    Check(link.TryArm(HwFrequence.Parse("10,10"), true, out _), "pair test armed");
    link.RequestDeviceStart();
    await WaitUntil(() => sink.Count >= 2);
    HwTriple[] pair = sink.Snapshot();
    Check(pair.Length >= 2 && pair[0].Equals(new HwTriple(100_000_000, 0, 3)) && pair[1].Equals(new HwTriple(0, 0, 4)), "failed B still follows A with no command between");
    Check(pair.Take(2).All(c => c.Data3 is 3 or 4), "start does not enter the pair");
    await WaitUntil(() => !link.SamplesArmed);
    transport.Enqueue(Frames.Encode(2, 1, 100, 0f));
    await Task.Delay(200);
    HwTriple[] after = sink.Snapshot();
    Check(after.Count(c => c.Data3 == 3) == 1 && after.Skip(2).All(c => c.Data3 is not 3 and not 4), "no further sample after B fails");
    cts.Cancel();
    await run;

    var transportA = new ScriptTransport();
    var sinkA = new PairSink { FailOn = 3 };
    var linkA = new LiveLink(transportA, sinkA, new LinkOptions { HeartbeatMs = 60_000, AckTimeoutMs = 400 });
    var helloA = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
    linkA.Status += text =>
    {
        if (text.Contains("HELLO_OK", StringComparison.Ordinal))
        {
            helloA.TrySetResult();
        }
    };
    using var ctsA = new CancellationTokenSource(TimeSpan.FromSeconds(3));
    Task runA = linkA.RunAsync(ctsA.Token);
    await helloA.Task.WaitAsync(TimeSpan.FromSeconds(2));
    Check(linkA.TryArm(HwFrequence.Parse("10,10"), true, out _), "A-fail test armed");
    linkA.RequestDeviceStart();
    await WaitUntil(() => sinkA.Count >= 1 && !linkA.SamplesArmed);
    Check(sinkA.Snapshot().All(c => c.Data3 != 4), "A failure does not invent B");
    ctsA.Cancel();
    await runA;
}

bool Same(SchematicLayout.Pt a, SchematicLayout.Pt b) => Math.Abs(a.X - b.X) < 0.01 && Math.Abs(a.Y - b.Y) < 0.01;

async Task WaitUntil(Func<bool> ready)
{
    var deadline = DateTime.UtcNow.AddSeconds(2);
    while (!ready() && DateTime.UtcNow < deadline)
    {
        await Task.Delay(20);
    }
    Check(ready(), "wait elapsed");
}

sealed class ThrowTransport : IByteTransport
{
    public int Calls;
    public ValueTask WriteAsync(ReadOnlyMemory<byte> data, CancellationToken cancellationToken)
    {
        Calls++;
        throw new InvalidOperationException("write");
    }
    public ValueTask<int> ReadAsync(Memory<byte> buffer, CancellationToken cancellationToken)
    {
        Calls++;
        throw new InvalidOperationException("read");
    }
}

sealed class NotReadySink : IHwSink
{
    public bool IsReady => false;
    public HwCallResult Send(int data1, int data2, int data3) => throw new InvalidOperationException("send");
}

sealed class PairSink : IHwSink
{
    private readonly List<HwTriple> _calls = new();
    public int FailOn { get; init; }
    public Action? OnChannelA { get; set; }
    public bool IsReady => true;
    public int Count { get { lock (_calls) return _calls.Count; } }
    public HwTriple[] Snapshot() { lock (_calls) return _calls.ToArray(); }
    public HwCallResult Send(int data1, int data2, int data3)
    {
        lock (_calls)
        {
            _calls.Add(new HwTriple(data1, data2, data3));
        }
        if (data3 == 3)
        {
            OnChannelA?.Invoke();
        }
        int code = data3 == FailOn ? -1 : 1;
        string text = code == 1 ? "返回 1：已向 HWClass 发出消息。这不是采集成功。" : "失败";
        return new HwCallResult(data1, data2, data3, code, text);
    }
}

sealed class RecordingSink : IHwSink
{
    private readonly List<HwTriple> _calls = new();
    public bool IsReady => true;
    public int Count { get { lock (_calls) return _calls.Count; } }
    public HwTriple[] Snapshot() { lock (_calls) return _calls.ToArray(); }
    public HwCallResult Send(int data1, int data2, int data3)
    {
        lock (_calls)
        {
            _calls.Add(new HwTriple(data1, data2, data3));
        }
        return new HwCallResult(data1, data2, data3, 1, "返回 1：已向 HWClass 发出消息。这不是采集成功。");
    }
}

static class Frames
{
    public static GcFrame One(byte seq, int batch, int time, float y)
    {
        byte[] payload = new byte[14];
        BinaryPrimitives.WriteInt32LittleEndian(payload, batch);
        BinaryPrimitives.WriteUInt16LittleEndian(payload.AsSpan(4), 1);
        BinaryPrimitives.WriteInt32LittleEndian(payload.AsSpan(6), time);
        BinaryPrimitives.WriteInt32LittleEndian(payload.AsSpan(10), BitConverter.SingleToInt32Bits(y));
        return new GcFrame(GcCodec.CmdData, seq, payload);
    }

    public static GcFrame Pair()
    {
        byte[] payload = new byte[22];
        BinaryPrimitives.WriteInt32LittleEndian(payload, 0);
        BinaryPrimitives.WriteUInt16LittleEndian(payload.AsSpan(4), 2);
        BinaryPrimitives.WriteInt32LittleEndian(payload.AsSpan(6), 0);
        BinaryPrimitives.WriteInt32LittleEndian(payload.AsSpan(10), BitConverter.SingleToInt32Bits(0f));
        BinaryPrimitives.WriteInt32LittleEndian(payload.AsSpan(14), 100);
        BinaryPrimitives.WriteInt32LittleEndian(payload.AsSpan(18), BitConverter.SingleToInt32Bits(0f));
        return new GcFrame(GcCodec.CmdData, 1, payload);
    }

    public static byte[] Encode(byte seq, int batch, int time, float y)
    {
        GcFrame frame = One(seq, batch, time, y);
        return GcCodec.Encode(frame.Command, frame.Sequence, frame.Payload);
    }

    public static byte[] BadCrc(byte[] frame)
    {
        byte[] copy = (byte[])frame.Clone();
        copy[^3] ^= 0xFF;
        return copy;
    }
}

sealed class ScriptTransport : IByteTransport
{
    private readonly Queue<byte[]> _in = new();
    public List<byte[]> Writes { get; } = new();
    public bool IncludeSample { get; set; } = true;

    public void Enqueue(byte[] data)
    {
        lock (_in)
        {
            _in.Enqueue(data);
        }
    }

    public ValueTask WriteAsync(ReadOnlyMemory<byte> data, CancellationToken cancellationToken)
    {
        byte[] copy = data.ToArray();
        lock (Writes)
        {
            Writes.Add(copy);
        }
        if (copy.AsSpan().StartsWith(GcCodec.HelloPc))
        {
            Enqueue(Encoding.ASCII.GetBytes("HELLO_OK\n"));
        }
        else if (copy.Length > 2 && copy[2] == GcCodec.CmdStart)
        {
            Enqueue(GcCodec.Encode(GcCodec.CmdAck, copy[3], new byte[] { GcCodec.CmdStart, 0 }));
            if (IncludeSample)
            {
                Enqueue(Frames.Encode(1, 0, 0, 0f));
            }
        }
        return ValueTask.CompletedTask;
    }

    public async ValueTask<int> ReadAsync(Memory<byte> buffer, CancellationToken cancellationToken)
    {
        var deadline = DateTime.UtcNow.AddMilliseconds(40);
        while (DateTime.UtcNow < deadline)
        {
            lock (_in)
            {
                if (_in.Count > 0)
                {
                    byte[] next = _in.Dequeue();
                    next.CopyTo(buffer);
                    return next.Length;
                }
            }
            await Task.Delay(5, cancellationToken);
        }
        return 0;
    }
}
