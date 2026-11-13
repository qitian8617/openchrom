#include "hs.h"
#include "live.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef _WIN32
#include <direct.h>
#include <winsock2.h>
#include <ws2tcpip.h>
#include <windows.h>
#define hs_mkdir(path) _mkdir(path)
#else
#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <unistd.h>
#define hs_mkdir(path) mkdir((path), 0700)
#endif

static int g_failed;
static int g_checked;
static char g_root[260];

static void expect(int cond, const char *name) {
	g_checked++;
	if (!cond) {
		fprintf(stderr, "FAIL %s\n", name);
		g_failed++;
	} else {
		printf("PASS %s\n", name);
	}
}

static void join_path(char *out, size_t cap, const char *dir, const char *leaf) {
	size_t dir_len;
	size_t leaf_len;
	if (out == NULL || cap == 0) {
		return;
	}
	if (dir == NULL) {
		dir = "";
	}
	if (leaf == NULL) {
		leaf = "";
	}
	dir_len = strlen(dir);
	leaf_len = strlen(leaf);
	if (dir_len + 1 + leaf_len >= cap) {
		out[0] = '\0';
		return;
	}
	memcpy(out, dir, dir_len);
	out[dir_len] = '/';
	memcpy(out + dir_len + 1, leaf, leaf_len);
	out[dir_len + 1 + leaf_len] = '\0';
}

static void test_path(char *out, size_t cap, const char *name) {
	snprintf(out, cap, "%s/hwstation-%s", g_root, name);
#ifdef _WIN32
	{
		char *slash = out;
		while (*slash) {
			if (*slash == '/') {
				*slash = '\\';
			}
			slash++;
		}
	}
#endif
	hs_mkdir(out);
}

static char *read_file(const char *path) {
	FILE *fp = fopen(path, "rb");
	long size;
	char *buf;
	if (!fp) {
		return NULL;
	}
	fseek(fp, 0, SEEK_END);
	size = ftell(fp);
	fseek(fp, 0, SEEK_SET);
	buf = malloc((size_t)size + 1);
	if (!buf) {
		fclose(fp);
		return NULL;
	}
	if (fread(buf, 1, (size_t)size, fp) != (size_t)size) {
		free(buf);
		fclose(fp);
		return NULL;
	}
	buf[size] = '\0';
	fclose(fp);
	return buf;
}

static void test_commands(void) {
	expect(hs_classify_command("GCWS\tSTOP_OVEN_CONTROL") == HS_CMD_SAFETY, "safety");
	expect(hs_classify_command("gcws\twrite_oven_pid") == HS_CMD_ENGINEERING, "pid case");
	expect(hs_classify_command("GCWS\tREAD_OVEN_TEMP\tGCWS\tSTOP_OVEN_CONTROL") == HS_CMD_UNSUPPORTED, "smuggle");
	expect(hs_classify_command("GCWS\tFORMAT_DISK") == HS_CMD_UNSUPPORTED, "unknown");
	expect(hs_command_allowed_when_locked("GCWS\tSTOP_FID_VALVES") == 1, "stop locked");
	expect(hs_command_allowed_when_locked("GCWS\tWRITE_OVEN_PID") == 0, "pid locked");
	expect(hs_payload_ok("{\"kp\":\"1\"}") == 1, "payload");
	expect(hs_payload_ok("GCWS\tSTOP") == 0, "payload smuggle");
	expect(hs_binary_opcode_ok(0x01) == 1, "start acq opcode");
	expect(hs_binary_opcode_ok(0x99) == 0, "bad opcode");
}

static void test_pressure_and_flame(void) {
	expect(hs_heat_start_gate(1, 0.1, 1, 0.1, 1000, 1000) == HS_HEAT_ALLOW, "0.1 exact");
	expect(hs_heat_start_gate(1, 0.099, 1, 0.2, 1000, 1000) == HS_HEAT_BLOCK_BELOW, "below");
	expect(hs_heat_start_gate(0, 0, 1, 0.2, 1000, 1000) == HS_HEAT_BLOCK_UNKNOWN, "unknown");
	expect(hs_heat_start_gate(1, 0.2, 1, 0.2, 1000, 5000) == HS_HEAT_BLOCK_STALE, "stale");
	expect(strcmp(hs_flame_text(HS_FLAME_UNKNOWN), "未知") == 0, "flame unknown");
	expect(strcmp(hs_flame_text(hs_flame_from_status(0, 1, 0, 1, "flame", 0, 0)), "未知") == 0, "missing not on");
	expect(hs_flame_from_status(1, 1, 0, 0, "idle", 1000, 1000) == HS_FLAME_OUT, "out");
	expect(hs_flame_from_status(1, 1, 0, 1, "idle", 1000, 9000) == HS_FLAME_STALE, "flame stale");
}

static void test_units(void) {
	HsFaMap map = hs_map_linear_pa_to_fa(1.234);
	char why[160];
	expect(map.status == HS_MAP_OK && map.fa == 1234, "1.234 pA");
	map = hs_map_linear_pa_to_fa(2147483.648);
	expect(map.status == HS_MAP_OVERFLOW && map.fa == 0, "no clamp");
	map = hs_map_sample_to_fa(HS_UNIT_VLOG_VOLT, 0.2);
	expect(map.status == HS_MAP_VLOG_NOT_LINEAR, "vlog not linear");
	map = hs_map_sample_to_fa(HS_UNIT_UNKNOWN, 1);
	expect(map.status == HS_MAP_UNKNOWN_UNIT, "unknown unit");
	map = hs_vlog_volts_to_fa(0.0);
	expect(map.status == HS_MAP_OK && map.fa == 100000000, "vlog 0 V formula");
	expect(hs_hw_interval_matches(100, 50, 1, why, sizeof why) == 0, "10 Hz blocked");
	expect(strstr(why, "HWFrequence") != NULL, "frequence blocked");
	expect(hs_hw_interval_matches(50, 50, 0, why, sizeof why) == 0, "unconfirmed 50");
	expect(hs_hw_interval_matches(50, 50, 1, why, sizeof why) == 1, "confirmed 50");
	expect(hs_hw_interval_matches(100, 100, 1, why, sizeof why) == 1, "confirmed 100");
	expect(hs_hw_export_blocked(41) == 1 && hs_hw_export_blocked(0) == 0 && hs_hw_export_blocked(51) == 0, "export gap");
}

static void test_stream(void) {
	HsStream *stream = hs_stream_new();
	HsSample sample;
	int32_t a, b, c;
	char note[160];
	memset(&sample, 0, sizeof sample);
	sample.unit = HS_UNIT_LINEAR_PA;
	sample.value = 1.234;
	sample.has_count = 1;
	sample.channel = 0;
	sample.t_ms = 0;
	sample.count = 1;
	expect(hs_stream_push(stream, &sample) == 0, "push 1");
	sample.t_ms = 100;
	sample.count = 2;
	expect(hs_stream_push(stream, &sample) == 0, "push 2");
	expect(hs_stream_next_hw_triple(stream, 50, 1, &a, &b, &c, note, sizeof note) == 0, "100 ms not pushed");
	expect(strstr(note, "50") != NULL, "reason shown");
	hs_stream_free(stream);
	stream = hs_stream_new();
	hs_stream_set_channel_mode(stream, 0);
	sample.t_ms = 0;
	sample.count = 1;
	hs_stream_push(stream, &sample);
	sample.t_ms = 50;
	sample.count = 2;
	hs_stream_push(stream, &sample);
	expect(hs_stream_next_hw_triple(stream, 50, 1, &a, &b, &c, note, sizeof note) == 1, "50 ms triple");
	expect(a == 1234 && b == 0 && c == 3, "placeholder");
	expect(strstr(note, "占位") != NULL, "placeholder note");
	sample.t_ms = 40;
	sample.count = 3;
	expect(hs_stream_push(stream, &sample) != 0, "reorder");
	expect(hs_stream_aborted(stream) == 1, "aborted");
	hs_stream_free(stream);
	stream = hs_stream_new();
	sample.t_ms = 0;
	sample.count = 1;
	sample.channel = 0;
	hs_stream_push(stream, &sample);
	sample.channel = 0;
	sample.t_ms = 50;
	sample.count = 2;
	hs_stream_push(stream, &sample);
	expect(hs_stream_next_hw_triple(stream, 50, 1, &a, &b, &c, note, sizeof note) == 0, "dual missing not zero-filled");
	expect(strstr(note, "未把缺通道填 0") != NULL, "dual note");
	hs_stream_free(stream);
}

static void test_frame(void) {
	uint8_t frame[32];
	uint8_t payload[64];
	unsigned cmd = 0;
	unsigned seq = 0;
	int len = 0;
	int n = hs_frame_encode(0x03, 1, NULL, 0, frame, sizeof frame);
	HsTriple triple;
	uint8_t packed[12];
	uint32_t magic = 0;
	uint32_t cb = 0;
	int times[2] = {0, 100};
	float signals[2] = {1.5f, 2.5f};
	int got_times[2];
	float got_signals[2];
	int count = 0;
	int batch = 0;
	expect(n == 10, "heartbeat size");
	expect(frame[6] == 0x51 && frame[7] == 0xA0, "crc");
	expect(hs_frame_decode(frame, n, &cmd, &seq, payload, sizeof payload, &len) == 0, "decode");
	expect(cmd == 0x03 && seq == 1 && len == 0, "heartbeat fields");
	frame[6] ^= 0xFF;
	expect(hs_frame_decode(frame, n, &cmd, &seq, payload, sizeof payload, &len) != 0, "bad crc");
	triple.data1 = -2;
	triple.data2 = 1234;
	triple.data3 = 1;
	hs_copydata_image(&triple, &magic, &cb, packed);
	expect(magic == 0xCCCC && cb == 12, "copydata");
	expect(hs_unpack_triple(packed, &triple) == 0 && triple.data1 == -2 && triple.data2 == 1234 && triple.data3 == 1, "signed triple");
	expect(strcmp(hs_sdk_return_meaning(1), hs_sdk_return_meaning(0)) != 0, "return text");
	expect(strstr(hs_sdk_return_meaning(1), "不表示") != NULL, "return 1 meaning");
	n = hs_acq_encode_batch(7, times, signals, 2, payload, sizeof payload);
	expect(n > 0 && hs_acq_decode_batch(payload, n, &batch, got_times, got_signals, 2, &count) == 0, "batch");
	expect(batch == 7 && count == 2 && got_times[1] == 100 && got_signals[0] == 1.5f, "batch values");
}

static void test_program(void) {
	double rate[8] = {0, 10, 0, 0, 0, 0, 0, 0};
	double temp[8] = {50, 80, 80, 80, 80, 80, 80, 80};
	double hold[8] = {1, 1, 0, 0, 0, 0, 0, 0};
	char json[1600];
	int n = hs_oven_program_json(450, 1, 30, 0, rate, temp, hold, json, sizeof json);
	expect(n > 0 && strstr(json, "\"loop\": \"0\"") != NULL, "oven json");
	expect(hs_oven_program_json(451, 1, 30, 0, rate, temp, hold, json, sizeof json) < 0, "ceiling");
	{
		double nan_setpoint;
		uint64_t nan_bits = 0x7ff8000000000000ULL;
		memcpy(&nan_setpoint, &nan_bits, sizeof nan_setpoint);
		expect(hs_setpoint_ok(nan_setpoint) == 0, "nan setpoint");
	}
}

static void test_auth_and_session(void) {
	uint8_t out[32];
	uint8_t expect_hash[32];
	static const char b64[] = "mhf19FrHRq6/OZJ3/iFi+M/rLZ/4hs9097fLeIBg+wM=";
	char dir[260];
	char pace_dir[320];
	char path[320];
	HsAuth *auth;
	HsSession *session;
	HsSnapshot snap;
	char err[200];
	int before;
	int attempts;
	test_path(dir, sizeof dir, "test-auth");
	snprintf(path, sizeof path, "%s/engineer.auth", dir);
	remove(path);
	expect(hs_pbkdf2_sha256((const uint8_t *)"test-password", 13, (const uint8_t *)"0123456789abcdef", 16, 2, out) == 0, "pbkdf2");
	{
		int val[4];
		int k = 0;
		size_t n = 0;
		const char *in = b64;
		memset(expect_hash, 0, sizeof expect_hash);
		while (*in && *in != '=') {
			const char *tab = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
			const char *hit = strchr(tab, *in++);
			val[k++] = hit ? (int)(hit - tab) : -1;
			if (k == 4) {
				expect_hash[n++] = (uint8_t)((val[0] << 2) | (val[1] >> 4));
				expect_hash[n++] = (uint8_t)((val[1] << 4) | (val[2] >> 2));
				expect_hash[n++] = (uint8_t)((val[2] << 6) | val[3]);
				k = 0;
			}
		}
		if (k == 3) {
			expect_hash[n++] = (uint8_t)((val[0] << 2) | (val[1] >> 4));
			expect_hash[n++] = (uint8_t)((val[1] << 4) | (val[2] >> 2));
		}
		(void)n;
	}
	expect(memcmp(out, expect_hash, 32) == 0, "matches java pbkdf2");
	auth = hs_auth_open(path);
	expect(hs_auth_missing(auth) == 1, "missing");
	expect(hs_auth_allows(auth, 0) == 0, "no bypass");
	expect(hs_auth_enroll(auth, "short", "short", 0, 210000) != 0, "short password");
	expect(hs_auth_enroll(auth, "correct-horse", "correct-horse", 1000, 210000) == 0, "enroll");
	hs_auth_lock(auth, 1000);
	expect(hs_auth_allows(auth, 1000) == 0, "locked after");
	hs_auth_set_idle_ms(auth, 1000);
	expect(hs_auth_verify(auth, "correct-horse", 5000) == 0, "verify");
	expect(hs_auth_allows(auth, 5999) == 1, "inside idle");
	expect(hs_auth_allows(auth, 6000) == 0, "idle lock");
	{
		char *text = read_file(path);
		expect(text && strstr(text, "correct-horse") == NULL, "no plaintext");
		expect(text && strstr(text, "password=") == NULL, "no password key");
		free(text);
	}
	hs_auth_close(auth);
	before = hs_live_attempt_count();
	session = hs_session_create(dir);
	expect(hs_session_net_writes(session) == 0, "create no net");
	expect(hs_session_connect_live(session, "", 8234, err, sizeof err) != 0, "empty host");
	expect(hs_session_connect_live(session, "192.168.0.8", 0, err, sizeof err) != 0, "empty port");
	expect(hs_live_attempt_count() == before, "no socket on bad host");
	expect(hs_session_start_heat(session, 1, 1, 1, 200, 250, 0, err, sizeof err) == 0, "sim heat");
	expect(hs_session_net_writes(session) == 0, "sim heat offline net");
	hs_session_set_sim_pressure(session, 0.05, 0.2, 1);
	hs_session_tick(session, 1000);
	expect(hs_session_start_heat(session, 1, 0, 0, 200, 0, 1000, err, sizeof err) != 0, "low pressure");
	expect(strstr(err, "0.1") != NULL, "pressure text");
	hs_session_set_offline(session);
	expect(hs_session_stop_heat(session, 1000, err, sizeof err) != 0, "offline stop");
	expect(strstr(err, "未确认") != NULL, "unconfirmed");
	expect(hs_session_net_writes(session) == 0, "offline stop no net");
	hs_session_set_sim(session);
	hs_session_set_sim_flame(session, HS_FLAME_OUT);
	expect(hs_session_start_device_acq(session, err, sizeof err) == 0, "acq without flame");
	expect(hs_session_net_writes(session) == 0, "sim acq no net");
	hs_session_tick(session, 0);
	hs_session_tick(session, 100);
	{
		HsTriple triple;
		char note[120];
		expect(hs_session_hw_start(session, &triple, err, sizeof err) != 0, "100 ms hw blocked");
		expect(hs_session_poll_hw_sample(session, &triple, note, sizeof note) == 0, "no fake points");
	}
	hs_session_set_sim_period_ms(session, 50);
	hs_session_free(session);
	test_path(pace_dir, sizeof pace_dir, "test-pace");
	session = hs_session_create(pace_dir);
	hs_session_free(session);
	session = hs_session_create(pace_dir);
	hs_session_set_sim_period_ms(session, 50);
	hs_session_tick(session, 0);
	hs_session_tick(session, 50);
	{
		HsTriple triple;
		char note[160];
		expect(hs_session_set_hw_interval(session, 50, 1, err, sizeof err) == 0, "interval confirmed");
		expect(hs_session_hw_start(session, &triple, err, sizeof err) == 0, "hw start composed");
		expect(triple.data3 == 1 && triple.data1 == 1, "data3 start");
		expect(hs_session_poll_hw_sample(session, &triple, note, sizeof note) == 1, "sample triple");
		expect(triple.data3 == 3 && triple.data1 == 1234 && triple.data2 == 0, "fa point");
		expect(hs_session_hw_stop(session, err, sizeof err) == 0, "hw stop");
		expect(strstr(err, "不停热") != NULL, "hw stop not heat");
	}
	expect(hs_session_set_pid(session, 0, 1, 0, 0, err, sizeof err) == 0, "pid edit");
	expect(hs_session_write_pids_confirmed(session, "no", 0, err, sizeof err) != 0, "pid locked");
	expect(hs_session_net_writes(session) == 0, "pid no net");
	hs_session_snapshot(session, 50, &snap);
	expect(strcmp(snap.fid_pa, "未知") == 0, "fid not zero");
	join_path(path, sizeof path, pace_dir, "ControlPanelInfo.txt");
	expect(hs_session_write_report(session, path, 50) == 0, "report");
	{
		char *text = read_file(path);
		expect(text && strstr(text, "未知") != NULL, "report unknown");
		expect(text && strstr(text, "没有实机") != NULL, "report limit");
		free(text);
	}
	hs_session_free(session);
	{
		char recover[320];
		FILE *fp;
		test_path(recover, sizeof recover, "test-recover");
		join_path(path, sizeof path, recover, "recovery.txt");
		fp = fopen(path, "wb");
		fputs("stop_unconfirmed=1\n", fp);
		fclose(fp);
		session = hs_session_create(recover);
		expect(strstr(hs_session_recovery_banner(session), "未确认") != NULL, "recovery");
		expect(hs_session_net_writes(session) == 0, "recovery no net");
		hs_session_free(session);
	}
	attempts = hs_live_attempt_count();
	expect(attempts == before, "still no live socket");
	(void)attempts;
}

static void test_layout_and_sources(void) {
	HsRect rects[8];
	int widths[4];
	int mins[4] = {72, 80, 80, 96};
	int rows = hs_layout_buttons(rects, 8, 0, 0, 584, 88, 30, 6);
	int i;
	char *panel;
	char *bridge;
	char *repro;
	expect(rows > 1, "wraps at 600");
	for (i = 0; i < 8; i++) {
		expect(rects[i].w >= 88, "button width");
		expect(rects[i].x + rects[i].w <= 584, "not clipped");
	}
	expect(hs_layout_columns(widths, mins, 4, 584) == 1, "columns");
	expect(widths[0] + widths[1] + widths[2] + widths[3] == 584, "no trailing gap");
	panel = read_file("win/ctrlpanel.c");
	bridge = read_file("win/hwbridge.c");
	repro = read_file("win/ipc_repro.c");
	if (panel == NULL || bridge == NULL || repro == NULL) {
		printf("SKIP source scan; run from the hwstation source directory to check window wiring\n");
		free(panel);
		free(bridge);
		free(repro);
		return;
	}
	expect(panel && strstr(panel, "ControlPanelClass") != NULL, "class string");
	expect(panel && strstr(panel, "FindWindow") == NULL, "panel does not search HW");
	expect(panel && strstr(panel, "写柱箱PID") == NULL, "no placeholder pid button");
	expect(panel && strstr(panel, "确认并写入") != NULL, "pid confirm button");
	expect(panel && strstr(panel, "下载温程") != NULL, "program download");
	expect(panel && strstr(panel, "探测此地址") != NULL, "manual probe");
	expect(panel && strstr(panel, "设置口令") != NULL, "enroll button");
	expect(panel && strstr(panel, "--offline-smoke") != NULL, "offline smoke flag");
	expect(panel && strstr(panel, "hs_session_start_heat") != NULL, "heat wired");
	expect(panel && strstr(panel, "hs_session_write_pids_confirmed") != NULL, "pid wired");
	expect(panel && strstr(panel, "hs_session_stop_heat") != NULL, "stop wired");
	expect(panel && strstr(panel, "hs_session_hw_stop") != NULL, "workstation stop wired");
	expect(bridge && strstr(bridge, "FindWindow") == NULL, "bridge does not search HW");
	expect(repro && strstr(repro, "FindWindowA") != NULL, "repro is explicit");
	expect(repro && strstr(repro, "SendDataToHW41") == NULL, "41 not exported by repro");
	free(panel);
	free(bridge);
	free(repro);
}

static int pe_machine(const char *path, unsigned *machine) {
	FILE *fp = fopen(path, "rb");
	unsigned char dos[64];
	unsigned char pe[6];
	long offset;
	if (!fp) {
		return -1;
	}
	if (fread(dos, 1, 64, fp) != 64 || dos[0] != 'M' || dos[1] != 'Z') {
		fclose(fp);
		return -1;
	}
	offset = (long)dos[0x3C] | ((long)dos[0x3D] << 8) | ((long)dos[0x3E] << 16) | ((long)dos[0x3F] << 24);
	if (fseek(fp, offset, SEEK_SET) != 0 || fread(pe, 1, 6, fp) != 6 || pe[0] != 'P' || pe[1] != 'E') {
		fclose(fp);
		return -1;
	}
	fclose(fp);
	*machine = (unsigned)pe[4] | ((unsigned)pe[5] << 8);
	return 0;
}

static int pe_named(const char *leaf, unsigned *machine) {
	char path[4][520];
	int i;
	memset(path, 0, sizeof path);
	snprintf(path[0], sizeof path[0], "build/win/%s", leaf);
	snprintf(path[1], sizeof path[1], "dist-msvc/%s", leaf);
	snprintf(path[2], sizeof path[2], "%s", leaf);
#ifdef _WIN32
	{
		char exe[MAX_PATH];
		DWORD n = GetModuleFileNameA(NULL, exe, MAX_PATH);
		char *slash;
		if (n > 0 && n < MAX_PATH) {
			slash = strrchr(exe, '\\');
			if (slash == NULL) {
				slash = strrchr(exe, '/');
			}
			if (slash != NULL) {
				*slash = '\0';
				snprintf(path[3], sizeof path[3], "%s\\%s", exe, leaf);
			}
		}
	}
#endif
	for (i = 0; i < 4; i++) {
		if (path[i][0] != '\0' && pe_machine(path[i], machine) == 0) {
			return 0;
		}
	}
	return -1;
}

static void test_binaries(void) {
	unsigned machine = 0;
	if (pe_named("hwbridge.exe", &machine) == 0) {
		expect(machine == 0x14C, "bridge i386");
	}
	if (pe_named("CtrlPanel.exe", &machine) == 0) {
		expect(machine == 0x8664, "panel x64");
	}
	if (pe_named("hwstation_ipc_repro.dll", &machine) == 0) {
		expect(machine == 0x14C, "repro dll i386");
	}
}

#ifdef _WIN32
static SOCKET g_listen_fd = INVALID_SOCKET;

static DWORD WINAPI loop_accept(LPVOID unused) {
	char buf[128];
	int n;
	SOCKET client;
	(void)unused;
	client = accept(g_listen_fd, NULL, NULL);
	if (client == INVALID_SOCKET) {
		return 1;
	}
	n = recv(client, buf, (int)sizeof buf - 1, 0);
	if (n > 0) {
		buf[n] = '\0';
		if (strstr(buf, "HELLO\tPC")) {
			const char *ok = "HELLO_OK\n";
			send(client, ok, (int)strlen(ok), 0);
		}
	}
	closesocket(client);
	return 0;
}
#endif

static void test_loopback(void) {
	struct sockaddr_in addr;
#ifdef _WIN32
	int alen = (int)sizeof addr;
	SOCKET listen_fd;
	HANDLE thread;
	WSADATA data;
#else
	socklen_t alen = sizeof addr;
	int listen_fd;
	pid_t pid;
#endif
	char err[128];
	char dir[320];
	HsSession *session;
	unsigned short port;
#ifdef _WIN32
	if (WSAStartup(MAKEWORD(2, 2), &data) != 0) {
		expect(0, "winsock");
		return;
	}
	listen_fd = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
#else
	listen_fd = socket(AF_INET, SOCK_STREAM, 0);
#endif
	memset(&addr, 0, sizeof addr);
	addr.sin_family = AF_INET;
	addr.sin_addr.s_addr = htonl(0x7F000001);
	addr.sin_port = 0;
	expect(bind(listen_fd, (struct sockaddr *)&addr, (int)sizeof addr) == 0, "bind");
	expect(listen(listen_fd, 1) == 0, "listen");
	expect(getsockname(listen_fd, (struct sockaddr *)&addr, &alen) == 0, "port");
	port = ntohs(addr.sin_port);
#ifdef _WIN32
	g_listen_fd = listen_fd;
	thread = CreateThread(NULL, 0, loop_accept, NULL, 0, NULL);
	expect(thread != NULL, "accept thread");
#else
	pid = fork();
	if (pid == 0) {
		int client = accept(listen_fd, NULL, NULL);
		char buf[128];
		int n = client >= 0 ? (int)read(client, buf, sizeof buf - 1) : -1;
		if (n > 0) {
			buf[n] = '\0';
			if (strstr(buf, "HELLO\tPC")) {
				const char *ok = "HELLO_OK\n";
				if (write(client, ok, strlen(ok)) < 0) {
					close(client);
					_exit(1);
				}
			}
		}
		if (client >= 0) {
			close(client);
		}
		_exit(0);
	}
#endif
	test_path(dir, sizeof dir, "test-loop");
	session = hs_session_create(dir);
	expect(hs_session_connect_live(session, "127.0.0.1", (int)port, err, sizeof err) == 0, "loopback hello");
	expect(hs_session_net_writes(session) == (int)strlen("GCWS\tHELLO\tPC\r\n"), "only hello");
	expect(strstr(hs_session_written(session), "START_") == NULL, "hello does not start");
	hs_session_disconnect(session);
	hs_session_free(session);
#ifdef _WIN32
	closesocket(listen_fd);
	if (thread) {
		WaitForSingleObject(thread, 3000);
		CloseHandle(thread);
	}
	WSACleanup();
#else
	close(listen_fd);
	waitpid(pid, NULL, 0);
#endif
}

typedef struct ScriptQ {
	char chunk[24][800];
	int count;
	int cursor;
	char written[24000];
	int written_len;
} ScriptQ;

static int script_write(void *ctx, const void *buf, int len) {
	ScriptQ *queue = ctx;
	if (queue->written_len + len < (int)sizeof queue->written) {
		memcpy(queue->written + queue->written_len, buf, (size_t)len);
		queue->written_len += len;
		queue->written[queue->written_len] = '\0';
	}
	return len;
}

static int script_read(void *ctx, void *buf, int cap, int timeout_ms) {
	ScriptQ *queue = ctx;
	int n;
	(void)timeout_ms;
	if (queue->cursor >= queue->count) {
		return -1;
	}
	n = (int)strlen(queue->chunk[queue->cursor]);
	if (n >= cap) {
		n = cap - 1;
	}
	memcpy(buf, queue->chunk[queue->cursor], (size_t)n);
	queue->cursor++;
	return n;
}

static void script_add(ScriptQ *queue, const char *text) {
	snprintf(queue->chunk[queue->count], sizeof queue->chunk[0], "%s", text);
	queue->count++;
}

static int count_text(const char *hay, const char *needle) {
	int n = 0;
	const char *p = hay;
	size_t len;
	if (hay == NULL || needle == NULL) {
		return 0;
	}
	len = strlen(needle);
	while ((p = strstr(p, needle)) != NULL) {
		n++;
		p += len;
	}
	return n;
}

static void test_pressure_program_pid_stop(void) {
	HsPressureKind h2_kind;
	HsPressureKind air_kind;
	double h2 = 0;
	double air = 0;
	HsProgram program;
	HsSession *session;
	HsSnapshot snap;
	ScriptQ queue;
	HsIo io;
	char err[240];
	char confirm[240];
	char auth_path[320];
	int i;
	memset(&program, 0, sizeof program);
	hs_parse_gas_pressure("{\"h2\":\"0.250\",\"air\":\"0.300\"}", &h2, &air, &h2_kind, &air_kind);
	expect(h2_kind == HS_PRESSURE_NAMED_MPA && air_kind == HS_PRESSURE_NAMED_MPA, "named pressure");
	hs_parse_gas_pressure("{\"adc1\":\"0.500\",\"adc2\":\"0.400\"}", &h2, &air, &h2_kind, &air_kind);
	expect(h2_kind == HS_PRESSURE_ADC_UNIT_UNCONFIRMED && air == 0.4, "adc kept");
	{
		char ops[320];
		test_path(ops, sizeof ops, "test-ops");
		session = hs_session_create(ops);
	}
	hs_session_set_offline(session);
	expect(hs_session_ingest_pressure_json(session, "{\"adc1\":\"0.8\",\"adc2\":\"0.8\"}", 1000) == 0, "adc parsed");
	hs_session_snapshot(session, 1000, &snap);
	expect(strstr(snap.h2, "单位未确认") != NULL, "adc not mpa");
	expect(hs_session_start_heat(session, 1, 0, 0, 200, 0, 1000, err, sizeof err) != 0, "adc cannot open heat");
	expect(hs_session_net_writes(session) == 0, "adc gate no net");
	program.max_c = 450;
	program.eq_min = 1;
	program.stable_sec = 30;
	program.temperature[0] = 50;
	program.hold[0] = 1;
	program.rate[1] = 0;
	program.temperature[1] = 80;
	program.hold[1] = 1;
	expect(hs_program_check(&program, err, sizeof err) != 0, "zero rate ramp rejected");
	program.rate[1] = 10;
	expect(hs_program_check(&program, err, sizeof err) == 0, "ramp ok");
	expect(hs_program_duration_ms(&program) > 60000, "duration");
	expect(hs_session_set_program(session, &program, err, sizeof err) == 0, "program stored");
	hs_session_free(session);
	memset(&queue, 0, sizeof queue);
	io.write = script_write;
	io.read = script_read;
	io.ctx = &queue;
	for (i = 0; i < 3; i++) {
		script_add(&queue, "WRITE_OK\n");
	}
	script_add(&queue, "WRITE_OK\n");
	{
		char ops[320];
		test_path(ops, sizeof ops, "test-ops");
		session = hs_session_create_with_io(ops, &io);
	}
	hs_session_use_scripted_live(session);
	hs_session_observe_aux(session, 1000, 0, 0, 0, 0, 0);
	hs_session_ingest_pressure_json(session, "{\"h2\":\"0.200\",\"air\":\"0.200\"}", 1000);
	expect(hs_session_start_heat(session, 1, 0, 1, 200, 0, 1000, err, sizeof err) == 0, "aux start");
	expect(count_text(queue.written, "START_OVEN_CONTROL") == 0, "oven not early");
	for (i = 0; i < 9; i++) {
		expect(hs_session_observe_aux(session, 1000 + i, 1, 200, 0, 0, -1) == 0, "still waiting");
	}
	expect(hs_session_observe_aux(session, 2000, 1, 200, 0, 0, -1) == 1, "tenth starts oven");
	expect(count_text(queue.written, "START_OVEN_CONTROL") == 1, "one oven start");
	hs_session_free(session);
	memset(&queue, 0, sizeof queue);
	for (i = 0; i < 3; i++) {
		script_add(&queue, "WRITE_OK\n");
	}
	for (i = 0; i < 4; i++) {
		script_add(&queue, "WRITE_OK\n");
	}
	script_add(&queue, "{\"owner\":\"PC\",\"heat\":\"0\",\"acq\":\"0\"}\n");
	{
		char ops2[320];
		test_path(ops2, sizeof ops2, "test-ops2");
		session = hs_session_create_with_io(ops2, &io);
	}
	hs_session_use_scripted_live(session);
	hs_session_observe_aux(session, 1000, 0, 0, 0, 0, 0);
	hs_session_ingest_pressure_json(session, "{\"h2\":\"0.200\",\"air\":\"0.200\"}", 1000);
	expect(hs_session_start_heat(session, 1, 0, 1, 200, 0, 1000, err, sizeof err) == 0, "start before cancel");
	for (i = 0; i < 4; i++) {
		hs_session_observe_aux(session, 1100 + i, 1, 200, 0, 0, -1);
	}
	expect(hs_session_stop_heat(session, 1500, err, sizeof err) == 0, "stop confirmed");
	expect(strstr(err, "heat") != NULL, "stop cites status");
	for (i = 0; i < 12; i++) {
		expect(hs_session_observe_aux(session, 1600 + i, 1, 200, 0, 0, -1) == 0, "no late oven");
	}
	expect(count_text(queue.written, "START_OVEN_CONTROL") == 0, "cancelled wait");
	expect(hs_session_net_writes(session) == 0, "script is not a socket");
	hs_session_free(session);
	{
		char pid_dir[320];
		test_path(pid_dir, sizeof pid_dir, "test-pid");
		join_path(auth_path, sizeof auth_path, pid_dir, "engineer.auth");
	}
	remove(auth_path);
	{
		HsAuth *auth = hs_auth_open(auth_path);
		expect(hs_auth_enroll(auth, "correct-horse", "correct-horse", 10, 210000) == 0, "pid enroll");
		hs_auth_close(auth);
	}
	memset(&queue, 0, sizeof queue);
	for (i = 0; i < 3; i++) {
		script_add(&queue, "WRITE_OK\n");
	}
	{
		char pid_dir[320];
		test_path(pid_dir, sizeof pid_dir, "test-pid");
		session = hs_session_create_with_io(pid_dir, &io);
	}
	expect(hs_session_verify(session, "correct-horse", 20, err, sizeof err) == 0, "pid unlock");
	expect(hs_session_set_pid(session, 0, 1.5, 0.2, 0, err, sizeof err) == 0, "oven pid");
	expect(hs_session_set_pid(session, 1, 2, 0.1, 0.3, err, sizeof err) == 0, "inlet pid");
	expect(hs_session_set_pid(session, 2, 3, 0, 4, err, sizeof err) == 0, "det pid");
	expect(hs_session_set_pid(session, 0, 2, 0, 0, err, sizeof err) == 0, "edited oven pid");
	hs_session_use_scripted_live(session);
	expect(hs_session_write_pids_confirmed(session, "确认写入 PID 但数值不对", 30, err, sizeof err) != 0, "bad confirm");
	expect(queue.written_len == 0, "bad confirm wrote nothing");
	hs_session_pid_confirm_text(session, confirm, sizeof confirm);
	expect(hs_session_write_pids_confirmed(session, confirm, 30, err, sizeof err) == 0, "confirmed pid");
	expect(count_text(queue.written, "WRITE_OVEN_PID") == 1, "oven pid sent");
	expect(count_text(queue.written, "WRITE_INLET_PID") == 1, "inlet pid sent");
	expect(strstr(queue.written, "\"kp\": \"2\"") != NULL, "sent edited kp");
	hs_session_free(session);
}

static void test_lock_and_log(void) {
	{
		char lock_dir[320];
		test_path(lock_dir, sizeof lock_dir, "test-lock");
		expect(hs_instance_try_lock(lock_dir) == 0, "lock");
		expect(hs_instance_try_lock(lock_dir) != 0, "second lock");
		hs_instance_unlock(lock_dir);
		expect(hs_instance_try_lock(lock_dir) == 0, "relock");
		hs_instance_unlock(lock_dir);
	}
	{
		char log_dir[320];
		HsSession *session;
		HsSnapshot snap;
		int i;
		test_path(log_dir, sizeof log_dir, "test-log");
		session = hs_session_create(log_dir);
		for (i = 0; i < 20000; i++) {
			hs_session_trace(session, "0123456789abcdef0123456789abcdef01234567");
		}
		hs_session_snapshot(session, 0, &snap);
		expect(snap.alarm_on == 1, "log capped");
		expect(strstr(snap.alarm, "上限") != NULL, "log alarm");
		hs_session_free(session);
	}
}

int main(int argc, char **argv) {
#ifdef _WIN32
	DWORD n = GetTempPathA((DWORD)sizeof g_root, g_root);
	if (n == 0 || n >= sizeof g_root) {
		snprintf(g_root, sizeof g_root, ".");
	} else if (n > 0 && (g_root[n - 1] == '\\' || g_root[n - 1] == '/')) {
		g_root[n - 1] = '\0';
	}
#else
	snprintf(g_root, sizeof g_root, "/tmp");
#endif
	if (argc > 2 || (argc == 2 && strcmp(argv[1], "--offline") != 0 && strcmp(argv[1], "--self-test") != 0)) {
		fprintf(stderr, "usage: hwstation-tests [--offline]\nThis program only runs offline. It does not search HWClass and does not connect to an instrument.\n");
		return 2;
	}
#if defined(_MSC_VER)
	printf("compiler: MSVC %d\n", _MSC_VER);
#elif defined(__clang__)
	printf("compiler: clang %s\n", __clang_version__);
#elif defined(__GNUC__)
	printf("compiler: gcc %d.%d.%d\n", __GNUC__, __GNUC_MINOR__, __GNUC_PATCHLEVEL__);
#else
	printf("compiler: unknown\n");
#endif
	printf("mode: offline\n");
	printf("network: 127.0.0.1 loopback hello only\n");
	printf("window-search: none\n");
	test_commands();
	test_pressure_and_flame();
	test_units();
	test_stream();
	test_frame();
	test_program();
	test_auth_and_session();
	test_layout_and_sources();
	test_binaries();
	test_loopback();
	test_pressure_program_pid_stop();
	test_lock_and_log();
	printf("cases: %d\n", g_checked);
	printf("failed: %d\n", g_failed);
	if (g_failed) {
		return 1;
	}
	printf("ok\n");
	return 0;
}
