#include "hs.h"
#include "live.h"

#include <fcntl.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>

#ifdef _WIN32
#include <direct.h>
#include <io.h>
#else
#include <unistd.h>
#endif

struct HsSession {
	char dir[512];
	char host[64];
	int port;
	int mode;
	int connected;
	HsIo *io;
	HsLive *live;
	int net_writes;
	char written[12288];
	int written_len;
	double inlet_set, inlet_act, det_set, det_act, oven_set, oven_act;
	int inlet_known, det_known, oven_known;
	int64_t temp_ms;
	int heat_running;
	int want_inlet, want_det, want_oven;
	int oven_started;
	int inlet_stable_polls, det_stable_polls;
	int oven_fault;
	int64_t oven_ms;
	int pressure_known;
	int h2_kind;
	int air_kind;
	double h2, air;
	int64_t pressure_ms;
	char pressure_note[160];
	HsProgram program;
	HsPidRow pid[3];
	int hw_interval_ms;
	int hw_interval_ok;
	int oven_wait_cancelled;
	int64_t acq_mark_ms;
	int stopwatch_hold;
	char events[8][140];
	int event_n;
	double trace_inlet[96];
	double trace_det[96];
	double trace_oven[96];
	int trace_n;
	HsFlame flame;
	int fid_known;
	int fid_pa_known;
	int fid_pa;
	int64_t fid_ms;
	char owner[16];
	int owner_known;
	int device_acq;
	int stop_unconfirmed;
	char stop_text[120];
	int linear_armed;
	char hw_status[180];
	char alarm[240];
	int alarm_on;
	char banner[280];
	HsStream *stream;
	int sim_period;
	HsFlame sim_flame;
	uint32_t sim_count;
	int64_t last_sample_ms;
	HsAuth *auth;
	int log_bytes;
	int log_rotations;
	int log_stopped;
	int channel_single;
};

static void ensure_dir(const char *dir) {
#ifdef _WIN32
	_mkdir(dir);
#else
	mkdir(dir, 0700);
#endif
}

static void copy_text(char *dst, size_t cap, const char *src) {
	if (dst == NULL || cap == 0) {
		return;
	}
	snprintf(dst, cap, "%s", src ? src : "");
}

static void load_recovery(HsSession *session) {
	char path[560];
	FILE *fp;
	char line[256];
	snprintf(path, sizeof path, "%s/recovery.txt", session->dir);
	fp = fopen(path, "rb");
	if (!fp) {
		return;
	}
	while (fgets(line, sizeof line, fp)) {
		if (strncmp(line, "stop_unconfirmed=1", 18) == 0) {
			session->stop_unconfirmed = 1;
			copy_text(session->banner, sizeof session->banner, "上次停止未确认。不假定加热或气路已经停下。");
			copy_text(session->stop_text, sizeof session->stop_text, "未确认");
		}
	}
	fclose(fp);
}

static void write_recovery(HsSession *session) {
	char path[560];
	FILE *fp;
	snprintf(path, sizeof path, "%s/recovery.txt", session->dir);
	fp = fopen(path, "wb");
	if (!fp) {
		return;
	}
	fprintf(fp, "stop_unconfirmed=%d\nheat_running=%d\n", session->stop_unconfirmed ? 1 : 0, session->heat_running ? 1 : 0);
	fclose(fp);
}

static void log_line(HsSession *session, const char *line) {
	char path[560];
	FILE *fp;
	size_t len;
	if (session->log_stopped || line == NULL) {
		return;
	}
	len = strlen(line);
	if (session->log_bytes + (int)len > HS_LOG_CAP_BYTES) {
		session->log_rotations++;
		session->log_bytes = 0;
		if (session->log_rotations >= HS_LOG_FILES) {
			session->log_stopped = 1;
			session->alarm_on = 1;
			copy_text(session->alarm, sizeof session->alarm, "原始日志达到上限，已停止增长");
			return;
		}
	}
	snprintf(path, sizeof path, "%s/raw.log", session->dir);
	fp = fopen(path, session->log_rotations ? "ab" : "ab");
	if (!fp) {
		session->alarm_on = 1;
		copy_text(session->alarm, sizeof session->alarm, "原始日志无法写入");
		return;
	}
	fwrite(line, 1, len, fp);
	fwrite("\n", 1, 1, fp);
	fclose(fp);
	session->log_bytes += (int)len + 1;
}

static HsSession *session_new(const char *dir, HsIo *io) {
	HsSession *session = calloc(1, sizeof *session);
	char auth_path[560];
	if (session == NULL) {
		return NULL;
	}
	snprintf(session->dir, sizeof session->dir, "%s", dir ? dir : ".");
	ensure_dir(session->dir);
	session->mode = 0;
	session->io = io;
	session->stream = hs_stream_new();
	hs_stream_set_channel_mode(session->stream, 0);
	session->sim_period = 100;
	session->sim_flame = HS_FLAME_UNKNOWN;
	session->flame = HS_FLAME_UNKNOWN;
	session->oven_fault = 0;
	session->inlet_set = 200;
	session->det_set = 250;
	session->oven_set = 60;
	session->inlet_act = 25;
	session->det_act = 25;
	session->oven_act = 25;
	session->inlet_known = session->det_known = session->oven_known = 1;
	session->pressure_known = 1;
	session->h2_kind = HS_PRESSURE_NAMED_MPA;
	session->air_kind = HS_PRESSURE_NAMED_MPA;
	session->h2 = 0.250;
	session->air = 0.300;
	copy_text(session->pressure_note, sizeof session->pressure_note, "仿真气压，键名按 h2/air MPa");
	session->channel_single = 1;
	copy_text(session->owner, sizeof session->owner, "未知");
	copy_text(session->hw_status, sizeof session->hw_status, "未投递");
	copy_text(session->stop_text, sizeof session->stop_text, "未请求");
	snprintf(auth_path, sizeof auth_path, "%s/engineer.auth", session->dir);
	session->auth = hs_auth_open(auth_path);
	load_recovery(session);
	return session;
}

HsSession *hs_session_create(const char *dir) {
	return session_new(dir, NULL);
}

HsSession *hs_session_create_with_io(const char *dir, HsIo *io) {
	return session_new(dir, io);
}

void hs_session_free(HsSession *session) {
	if (session == NULL) {
		return;
	}
	hs_live_close(session->live);
	hs_stream_free(session->stream);
	hs_auth_close(session->auth);
	free(session);
}

int hs_instance_try_lock(const char *dir) {
	char path[560];
	int fd;
	if (dir == NULL) {
		return -1;
	}
	ensure_dir(dir);
	snprintf(path, sizeof path, "%s/single.lock", dir);
	fd = open(path, O_CREAT | O_EXCL | O_WRONLY, 0600);
	if (fd < 0) {
		return -1;
	}
	if (write(fd, "locked\n", 7) < 0) {
		close(fd);
		remove(path);
		return -1;
	}
	close(fd);
	return 0;
}

void hs_instance_unlock(const char *dir) {
	char path[560];
	if (dir == NULL) {
		return;
	}
	snprintf(path, sizeof path, "%s/single.lock", dir);
	remove(path);
}

static void clear_live_values(HsSession *session) {
	session->inlet_known = session->det_known = session->oven_known = 0;
	session->pressure_known = 0;
	session->h2_kind = HS_PRESSURE_ABSENT;
	session->air_kind = HS_PRESSURE_ABSENT;
	session->fid_known = 0;
	session->fid_pa_known = 0;
	session->owner_known = 0;
	session->oven_fault = -1;
	session->flame = HS_FLAME_UNKNOWN;
	copy_text(session->owner, sizeof session->owner, "未知");
}

int hs_session_set_offline(HsSession *session) {
	if (session == NULL) {
		return -1;
	}
	hs_live_close(session->live);
	session->live = NULL;
	session->connected = 0;
	session->mode = 1;
	clear_live_values(session);
	session->heat_running = 0;
	return 0;
}

int hs_session_set_sim(HsSession *session) {
	if (session == NULL) {
		return -1;
	}
	hs_live_close(session->live);
	session->live = NULL;
	session->connected = 0;
	session->mode = 0;
	session->pressure_known = 1;
	session->h2_kind = HS_PRESSURE_NAMED_MPA;
	session->air_kind = HS_PRESSURE_NAMED_MPA;
	session->h2 = 0.250;
	session->air = 0.300;
	session->inlet_known = session->det_known = session->oven_known = 1;
	session->oven_fault = 0;
	session->flame = session->sim_flame;
	return 0;
}

int hs_session_use_scripted_live(HsSession *session) {
	if (session == NULL || session->io == NULL) {
		return -1;
	}
	session->mode = 2;
	session->connected = 1;
	clear_live_values(session);
	return 0;
}

int hs_host_port_ok(const char *host, int port, char *err, size_t err_cap) {
	int parts[4];
	int index = 0;
	const char *p;
	if (err && err_cap) {
		err[0] = '\0';
	}
	if (host == NULL || host[0] == '\0' || strchr(host, ' ') || strchr(host, '/') || strchr(host, '\\')) {
		if (err && err_cap) {
			snprintf(err, err_cap, "主机必须手工填写 IPv4，不能为空");
		}
		return 0;
	}
	p = host;
	while (index < 4) {
		char *end = NULL;
		long value;
		if (*p < '0' || *p > '9') {
			break;
		}
		value = strtol(p, &end, 10);
		if (end == p || value < 0 || value > 255) {
			break;
		}
		parts[index++] = (int)value;
		p = end;
		if (index < 4) {
			if (*p != '.') {
				break;
			}
			p++;
		}
	}
	if (index != 4 || *p != '\0') {
		if (err && err_cap) {
			snprintf(err, err_cap, "只接受完整 IPv4，不解析主机名，不猜测地址");
		}
		return 0;
	}
	(void)parts;
	if (port <= 0 || port > 65535) {
		if (err && err_cap) {
			snprintf(err, err_cap, "端口必须手工填写 1–65535，不会自动填入 8234");
		}
		return 0;
	}
	return 1;
}

static int capture(HsSession *session, const void *buf, int len) {
	if (len < 0) {
		return -1;
	}
	if (session->written_len + len < (int)sizeof session->written) {
		memcpy(session->written + session->written_len, buf, (size_t)len);
		session->written_len += len;
		session->written[session->written_len] = '\0';
	}
	return 0;
}

static int transport_write(HsSession *session, const void *buf, int len) {
	capture(session, buf, len);
	log_line(session, "TX");
	if (session->live) {
		int n = hs_live_write(session->live, buf, len);
		if (n > 0) {
			session->net_writes += n;
		}
		return n == len ? 0 : -1;
	}
	if (session->io && session->io->write) {
		int n = session->io->write(session->io->ctx, buf, len);
		return n == len ? 0 : -1;
	}
	return -1;
}

static int transport_read(HsSession *session, char *buf, int cap) {
	int n;
	if (session->live) {
		n = hs_live_read(session->live, buf, cap - 1, 2000);
	} else if (session->io && session->io->read) {
		n = session->io->read(session->io->ctx, buf, cap - 1, 2000);
	} else {
		return -1;
	}
	if (n < 0) {
		return -1;
	}
	buf[n] = '\0';
	return n;
}

static int exchange(HsSession *session, const char *line, const char *payload, char *reply, size_t reply_cap, int64_t now_ms) {
	char framed[1800];
	int n;
	HsCmdKind kind = hs_classify_command(line);
	if (kind == HS_CMD_UNSUPPORTED || !hs_payload_ok(payload)) {
		return -1;
	}
	if (kind == HS_CMD_ENGINEERING && !hs_auth_allows(session->auth, now_ms)) {
		return -2;
	}
	n = snprintf(framed, sizeof framed, "%s\r\n", line);
	if (transport_write(session, framed, n) != 0) {
		return -1;
	}
	if (payload && payload[0]) {
		if (transport_write(session, payload, (int)strlen(payload)) != 0) {
			return -1;
		}
	}
	if (reply && reply_cap) {
		if (transport_read(session, reply, (int)reply_cap) < 0 || reply[0] == '\0') {
			return -5;
		}
		if (strstr(reply, "DENIED")) {
			return -3;
		}
		if (strstr(reply, "WRITE_FAIL")) {
			return -4;
		}
		if ((kind == HS_CMD_OPERATOR || kind == HS_CMD_SAFETY || kind == HS_CMD_ENGINEERING) && strstr(reply, "WRITE_OK") == NULL) {
			return -4;
		}
	}
	return 0;
}

static void push_event(HsSession *session, const char *text) {
	int i;
	if (session == NULL || text == NULL) {
		return;
	}
	if (session->event_n == 8) {
		for (i = 1; i < 8; i++) {
			memcpy(session->events[i - 1], session->events[i], sizeof session->events[0]);
		}
		session->event_n = 7;
	}
	snprintf(session->events[session->event_n], sizeof session->events[0], "%s", text);
	session->event_n++;
}

static void push_trace(HsSession *session) {
	if (session->trace_n >= 96) {
		memmove(session->trace_inlet, session->trace_inlet + 1, 95 * sizeof(double));
		memmove(session->trace_det, session->trace_det + 1, 95 * sizeof(double));
		memmove(session->trace_oven, session->trace_oven + 1, 95 * sizeof(double));
		session->trace_n = 95;
	}
	session->trace_inlet[session->trace_n] = session->inlet_known ? session->inlet_act : -1;
	session->trace_det[session->trace_n] = session->det_known ? session->det_act : -1;
	session->trace_oven[session->trace_n] = session->oven_known ? session->oven_act : -1;
	session->trace_n++;
}

int hs_session_connect_live(HsSession *session, const char *host, int port, char *err, size_t err_cap) {
	char reply[256];
	const char *hello = "GCWS\tHELLO\tPC\r\n";
	if (session == NULL) {
		return -1;
	}
	if (!hs_host_port_ok(host, port, err, err_cap)) {
		return -1;
	}
	if (hs_live_connect(&session->live, host, port, 3000, err, err_cap) != 0) {
		session->live = NULL;
		session->connected = 0;
		return -1;
	}
	session->mode = 2;
	snprintf(session->host, sizeof session->host, "%s", host);
	session->port = port;
	clear_live_values(session);
	if (hs_live_write(session->live, hello, (int)strlen(hello)) != (int)strlen(hello)) {
		session->net_writes += 0;
		hs_live_close(session->live);
		session->live = NULL;
		copy_text(err, err_cap, "握手发送失败");
		return -1;
	}
	session->net_writes += (int)strlen(hello);
	capture(session, hello, (int)strlen(hello));
	if (hs_live_read(session->live, reply, (int)sizeof reply - 1, 3000) < 0 || strstr(reply, "HELLO_OK") == NULL) {
		hs_live_close(session->live);
		session->live = NULL;
		copy_text(err, err_cap, "未收到 HELLO_OK，未保持连接");
		return -1;
	}
	session->connected = 1;
	copy_text(session->owner, sizeof session->owner, "PC?");
	session->owner_known = 0;
	return 0;
}

void hs_session_disconnect(HsSession *session) {
	if (session == NULL) {
		return;
	}
	hs_live_close(session->live);
	session->live = NULL;
	session->connected = 0;
	if (session->heat_running) {
		session->stop_unconfirmed = 1;
		copy_text(session->stop_text, sizeof session->stop_text, "断线，停止未确认");
		copy_text(session->banner, sizeof session->banner, "连接已断。加热或气路是否停下未知。");
		write_recovery(session);
	}
}

static int live_ready(const HsSession *session) {
	return session->mode == 2 && session->connected;
}

static int apply_pressure_json(HsSession *session, const char *json, int64_t now_ms) {
	HsPressureKind h2_kind = HS_PRESSURE_ABSENT;
	HsPressureKind air_kind = HS_PRESSURE_ABSENT;
	double h2 = 0;
	double air = 0;
	hs_parse_gas_pressure(json, &h2, &air, &h2_kind, &air_kind);
	session->h2 = h2;
	session->air = air;
	session->h2_kind = h2_kind;
	session->air_kind = air_kind;
	session->pressure_ms = now_ms;
	session->pressure_known = h2_kind != HS_PRESSURE_ABSENT && air_kind != HS_PRESSURE_ABSENT;
	if (h2_kind == HS_PRESSURE_NAMED_MPA && air_kind == HS_PRESSURE_NAMED_MPA) {
		copy_text(session->pressure_note, sizeof session->pressure_note, "h2/air，按现有界面记为 MPa");
	} else if (h2_kind == HS_PRESSURE_ADC_UNIT_UNCONFIRMED || air_kind == HS_PRESSURE_ADC_UNIT_UNCONFIRMED) {
		session->alarm_on = 1;
		copy_text(session->pressure_note, sizeof session->pressure_note, "只有 adc1/adc2，单位未确认，不能用于 0.1 MPa 门槛。需要固件以 h2/air 上报");
		copy_text(session->alarm, sizeof session->alarm, session->pressure_note);
	} else {
		copy_text(session->pressure_note, sizeof session->pressure_note, "气压缺失");
	}
	return session->pressure_known ? 0 : -1;
}

int hs_session_poll(HsSession *session) {
	char reply[1024];
	int64_t now = session ? session->pressure_ms : 0;
	if (session == NULL || !live_ready(session)) {
		return -1;
	}
	if (exchange(session, "GCWS\tREAD_GAS_PRESSURE", NULL, reply, sizeof reply, now) != 0) {
		session->pressure_known = 0;
		return -1;
	}
	return apply_pressure_json(session, reply, now);
}

void hs_session_tick(HsSession *session, int64_t now_ms) {
	HsSample sample;
	if (session == NULL || session->mode != 0) {
		return;
	}
	session->pressure_ms = now_ms;
	session->temp_ms = now_ms;
	session->flame = session->sim_flame;
	session->fid_ms = now_ms;
	if (session->heat_running) {
		if (session->want_inlet && session->inlet_act < session->inlet_set) {
			session->inlet_act += 5.0;
			if (session->inlet_act > session->inlet_set) {
				session->inlet_act = session->inlet_set;
			}
		}
		if (session->want_det && session->det_act < session->det_set) {
			session->det_act += 5.0;
			if (session->det_act > session->det_set) {
				session->det_act = session->det_set;
			}
		}
		if (session->want_oven && session->oven_started && session->oven_act < session->oven_set) {
			session->oven_act += 2.0;
			if (session->oven_act > session->oven_set) {
				session->oven_act = session->oven_set;
			}
		}
		hs_session_observe_aux(session, now_ms, session->want_inlet, session->inlet_act, session->want_det, session->det_act, session->oven_fault);
		push_trace(session);
	}
	if (session->last_sample_ms != 0 && now_ms - session->last_sample_ms < session->sim_period) {
		return;
	}
	memset(&sample, 0, sizeof sample);
	sample.t_ms = now_ms;
	sample.count = session->sim_count++;
	sample.has_count = 1;
	sample.value = 1.234;
	sample.unit = HS_UNIT_LINEAR_PA;
	sample.channel = 0;
	session->last_sample_ms = now_ms;
	hs_stream_push(session->stream, &sample);
}

static void num_or_unknown(char *dst, size_t cap, int known, int64_t sample_ms, int64_t now_ms, double value, const char *suffix) {
	if (!known) {
		copy_text(dst, cap, "未知");
		return;
	}
	if (now_ms - sample_ms > HS_STATUS_FRESH_MS || sample_ms > now_ms) {
		copy_text(dst, cap, "过期");
		return;
	}
	snprintf(dst, cap, "%.3f%s", value, suffix ? suffix : "");
}

void hs_session_snapshot(HsSession *session, int64_t now_ms, HsSnapshot *out) {
	if (out == NULL) {
		return;
	}
	memset(out, 0, sizeof *out);
	if (session == NULL) {
		return;
	}
	copy_text(out->mode, sizeof out->mode, session->mode == 0 ? "仿真" : session->mode == 1 ? "离线" : "在线");
	if (session->mode == 0) {
		copy_text(out->link, sizeof out->link, "仿真 · 未连接仪器");
	} else if (!session->connected) {
		copy_text(out->link, sizeof out->link, "未连接");
	} else {
		snprintf(out->link, sizeof out->link, "已连接 %s:%d", session->host, session->port);
	}
	if (session->stop_unconfirmed) {
		copy_text(out->heat, sizeof out->heat, "停止未确认");
	} else {
		copy_text(out->heat, sizeof out->heat, session->heat_running ? (session->oven_started || !session->want_oven ? "温控运行" : "温控运行 · 柱箱等待前置") : "温控停止");
	}
	copy_text(out->stop_confirm, sizeof out->stop_confirm, session->stop_text);
	copy_text(out->device_acq, sizeof out->device_acq, session->device_acq ? "设备采集中" : "设备采集停");
	copy_text(out->hw, sizeof out->hw, session->hw_status);
	copy_text(out->alarm, sizeof out->alarm, session->alarm);
	copy_text(out->banner, sizeof out->banner, session->banner);
	copy_text(out->flame, sizeof out->flame, hs_flame_text(session->flame));
	if (!session->fid_pa_known) {
		copy_text(out->fid_pa, sizeof out->fid_pa, "未知");
	} else if (now_ms - session->fid_ms > HS_STATUS_FRESH_MS) {
		copy_text(out->fid_pa, sizeof out->fid_pa, "过期");
	} else {
		snprintf(out->fid_pa, sizeof out->fid_pa, "%d（字段 currentPa）", session->fid_pa);
	}
	if (session->h2_kind == HS_PRESSURE_NAMED_MPA) {
		num_or_unknown(out->h2, sizeof out->h2, 1, session->pressure_ms, now_ms, session->h2, " MPa");
	} else if (session->h2_kind == HS_PRESSURE_ADC_UNIT_UNCONFIRMED) {
		snprintf(out->h2, sizeof out->h2, "adc %.3f 单位未确认", session->h2);
	} else {
		copy_text(out->h2, sizeof out->h2, "未知");
	}
	if (session->air_kind == HS_PRESSURE_NAMED_MPA) {
		num_or_unknown(out->air, sizeof out->air, 1, session->pressure_ms, now_ms, session->air, " MPa");
	} else if (session->air_kind == HS_PRESSURE_ADC_UNIT_UNCONFIRMED) {
		snprintf(out->air, sizeof out->air, "adc %.3f 单位未确认", session->air);
	} else {
		copy_text(out->air, sizeof out->air, "未知");
	}
	num_or_unknown(out->inlet_set, sizeof out->inlet_set, session->inlet_known, session->temp_ms, now_ms, session->inlet_set, "");
	num_or_unknown(out->inlet_act, sizeof out->inlet_act, session->inlet_known, session->temp_ms, now_ms, session->inlet_act, "");
	copy_text(out->inlet_state, sizeof out->inlet_state, session->heat_running && session->want_inlet ? "加热" : "停止");
	num_or_unknown(out->det_set, sizeof out->det_set, session->det_known, session->temp_ms, now_ms, session->det_set, "");
	num_or_unknown(out->det_act, sizeof out->det_act, session->det_known, session->temp_ms, now_ms, session->det_act, "");
	copy_text(out->det_state, sizeof out->det_state, session->heat_running && session->want_det ? "加热" : "停止");
	num_or_unknown(out->oven_set, sizeof out->oven_set, session->oven_known, session->temp_ms, now_ms, session->oven_set, "");
	num_or_unknown(out->oven_act, sizeof out->oven_act, session->oven_known, session->temp_ms, now_ms, session->oven_act, "");
	if (session->oven_fault < 0) {
		copy_text(out->oven_state, sizeof out->oven_state, "柱箱状态未知");
	} else if (session->oven_fault) {
		copy_text(out->oven_state, sizeof out->oven_state, "柱箱故障");
	} else if (session->want_oven && session->heat_running && !session->oven_started) {
		copy_text(out->oven_state, sizeof out->oven_state, "等待前置");
	} else {
		copy_text(out->oven_state, sizeof out->oven_state, session->oven_started ? "程序运行" : "停止");
	}
	copy_text(out->engineer, sizeof out->engineer, hs_auth_status(session->auth, now_ms));
	if (session->hw_interval_ok) {
		snprintf(out->pace, sizeof out->pace, "源周期 %d ms；已核实工作站间隔 %d ms；未写 HWFrequence.txt", hs_stream_period_ms(session->stream), session->hw_interval_ms);
	} else {
		snprintf(out->pace, sizeof out->pace, "源周期 %d ms；工作站间隔未核实（默认 50 ms 不是对接完成）", hs_stream_period_ms(session->stream));
	}
	copy_text(out->unit_note, sizeof out->unit_note, session->linear_armed ? "操作者确认在线浮点按线性 pA 解释（协议未证实）" : "在线采样量纲未确认，默认不映射");
	copy_text(out->owner, sizeof out->owner, session->owner);
	out->heat_running = session->heat_running;
	out->connected = session->connected || session->mode == 0;
	out->alarm_on = session->alarm_on || hs_stream_aborted(session->stream);
	out->net_writes = session->net_writes;
	out->sim = session->mode == 0;
	if (hs_stream_aborted(session->stream)) {
		copy_text(out->alarm, sizeof out->alarm, hs_stream_abort_reason(session->stream));
		out->alarm_on = 1;
	}
}

int hs_session_net_writes(const HsSession *session) {
	return session ? session->net_writes : 0;
}

static int heat_blocked(HsSession *session, int64_t now_ms, char *err, size_t err_cap) {
	int named = session->h2_kind == HS_PRESSURE_NAMED_MPA && session->air_kind == HS_PRESSURE_NAMED_MPA;
	HsHeatGate gate;
	if (!named) {
		copy_text(err, err_cap, session->pressure_note[0] ? session->pressure_note : "气压不是已确认的 h2/air，拒绝启动温控");
		return -1;
	}
	gate = hs_heat_start_gate(1, session->h2, 1, session->air, session->pressure_ms, now_ms);
	if (gate != HS_HEAT_ALLOW) {
		copy_text(err, err_cap, hs_heat_gate_text(gate));
		return -1;
	}
	return 0;
}

int hs_session_start_heat(HsSession *session, int inlet, int detector, int oven, double inlet_c, double detector_c, int64_t now_ms, char *err, size_t err_cap) {
	char reply[256];
	char json[128];
	int sel;
	if (session == NULL) {
		return -1;
	}
	if (session->mode == 1 || (session->mode == 2 && !session->connected)) {
		copy_text(err, err_cap, "未连接，未发送温控启动");
		return -1;
	}
	if ((inlet && !hs_setpoint_ok(inlet_c)) || (detector && !hs_setpoint_ok(detector_c))) {
		copy_text(err, err_cap, "目标温度无效或超过 450 °C");
		return -1;
	}
	if (!inlet && !detector && !oven) {
		copy_text(err, err_cap, "未选择进样口、检测器或柱箱");
		return -1;
	}
	if (heat_blocked(session, now_ms, err, err_cap)) {
		return -1;
	}
	if (oven && session->oven_fault < 0) {
		copy_text(err, err_cap, "柱箱状态未知，拒绝启动柱箱");
		return -1;
	}
	if (oven && session->oven_fault > 0) {
		copy_text(err, err_cap, "柱箱故障，拒绝启动柱箱");
		return -1;
	}
	session->want_inlet = inlet;
	session->want_det = detector;
	session->want_oven = oven;
	if (inlet) {
		session->inlet_set = inlet_c;
	}
	if (detector) {
		session->det_set = detector_c;
	}
	if (session->mode == 0) {
		session->heat_running = 1;
		session->oven_started = oven && !inlet && !detector;
		session->oven_wait_cancelled = 0;
		session->inlet_stable_polls = 0;
		session->det_stable_polls = 0;
		session->stop_unconfirmed = 0;
		copy_text(session->stop_text, sizeof session->stop_text, "仿真运行（不是仪器确认）");
		copy_text(err, err_cap, "仿真已启动温控，未发送网络数据");
		return 0;
	}
	sel = (inlet ? 1 : 0) | (detector ? 2 : 0) | (oven ? 4 : 0);
	snprintf(json, sizeof json, "{\"sel\":\"%d\"}", sel);
	if (exchange(session, "GCWS\tWRITE_ZONE_SELECT", json, reply, sizeof reply, now_ms) != 0) {
		copy_text(err, err_cap, "区选择失败，未继续启动");
		return -1;
	}
	if (inlet) {
		snprintf(json, sizeof json, "{\n  \"setpoint\": \"%.0f\"\n}\n", inlet_c);
		if (exchange(session, "GCWS\tWRITE_INLET_TEMP", json, reply, sizeof reply, now_ms) != 0 || exchange(session, "GCWS\tSTART_INLET_HEAT", NULL, reply, sizeof reply, now_ms) != 0) {
			copy_text(err, err_cap, "进样口加热未确认");
			session->stop_unconfirmed = 1;
			return -1;
		}
	}
	if (detector) {
		snprintf(json, sizeof json, "{\n  \"setpoint\": \"%.0f\"\n}\n", detector_c);
		if (exchange(session, "GCWS\tWRITE_DETECTOR_TEMP", json, reply, sizeof reply, now_ms) != 0 || exchange(session, "GCWS\tSTART_DETECTOR_HEAT", NULL, reply, sizeof reply, now_ms) != 0) {
			copy_text(err, err_cap, "检测器加热未确认");
			session->stop_unconfirmed = 1;
			return -1;
		}
	}
	session->oven_started = 0;
	if (oven && !inlet && !detector) {
		if (exchange(session, "GCWS\tSTART_OVEN_CONTROL", NULL, reply, sizeof reply, now_ms) != 0) {
			copy_text(err, err_cap, "柱箱启动未确认");
			return -1;
		}
		session->oven_started = 1;
	}
	session->heat_running = 1;
	session->oven_wait_cancelled = 0;
	session->inlet_stable_polls = 0;
	session->det_stable_polls = 0;
	session->temp_ms = now_ms;
	session->inlet_known = session->det_known = session->oven_known = 1;
	copy_text(err, err_cap, session->oven_started || !oven ? "温控命令已收到 WRITE_OK" : "辅助区已启动，柱箱等待前置恒温");
	return 0;
}

static int stop_commands(HsSession *session, int64_t now_ms, char *err, size_t err_cap) {
	char reply[512];
	int failed = 0;
	int heat_known = 0;
	int heat_on = 1;
	const char *lines[] = {"GCWS\tSTOP_INLET_HEAT", "GCWS\tSTOP_DETECTOR_HEAT", "GCWS\tSTOP_OVEN_CONTROL", "GCWS\tSTOP_FID_VALVES"};
	int i;
	session->oven_wait_cancelled = 1;
	session->heat_running = 0;
	session->oven_started = 0;
	for (i = 0; i < 4; i++) {
		reply[0] = '\0';
		if (exchange(session, lines[i], NULL, reply, sizeof reply, now_ms) != 0) {
			failed = 1;
		}
	}
	reply[0] = '\0';
	if (exchange(session, "GCWS\tREAD_CONTROL_STATUS", NULL, reply, sizeof reply, now_ms) != 0) {
		failed = 1;
	} else if (hs_parse_control_heat(reply, &heat_known, &heat_on, session->owner, sizeof session->owner) != 0 || !heat_known || heat_on) {
		failed = 1;
	}
	if (failed) {
		session->stop_unconfirmed = 1;
		copy_text(session->stop_text, sizeof session->stop_text, "未确认");
		copy_text(err, err_cap, "停止未确认：缺少 WRITE_OK、状态超时，或设备仍报告加热");
		push_event(session, "停止未确认");
		write_recovery(session);
		return -1;
	}
	session->stop_unconfirmed = 0;
	copy_text(session->stop_text, sizeof session->stop_text, "设备回报加热已停");
	copy_text(err, err_cap, "四路停止均 WRITE_OK，且 READ_CONTROL_STATUS 的 heat 为 0");
	push_event(session, "停止已由设备状态确认");
	write_recovery(session);
	return 0;
}

int hs_session_stop_heat(HsSession *session, int64_t now_ms, char *err, size_t err_cap) {
	(void)now_ms;
	if (session == NULL) {
		return -1;
	}
	if (session->mode == 1 || (session->mode == 2 && !session->connected)) {
		session->stop_unconfirmed = 1;
		session->heat_running = 0;
		copy_text(session->stop_text, sizeof session->stop_text, "未确认");
		copy_text(err, err_cap, "未连接，停止未确认，不保证设备已停");
		write_recovery(session);
		return -1;
	}
	if (session->mode == 0) {
		session->heat_running = 0;
		session->oven_started = 0;
		session->stop_unconfirmed = 0;
		copy_text(session->stop_text, sizeof session->stop_text, "仿真已停（不是仪器确认）");
		copy_text(err, err_cap, "仿真已停止，未发送网络数据");
		return 0;
	}
	return stop_commands(session, now_ms, err, err_cap);
}

int hs_session_estop(HsSession *session, int64_t now_ms, char *err, size_t err_cap) {
	int rc = hs_session_stop_heat(session, now_ms, err, err_cap);
	if (session) {
		copy_text(session->hw_status, sizeof session->hw_status, "急停未改工作站谱图投递");
	}
	return rc;
}

int hs_session_ignite(HsSession *session, char *err, size_t err_cap) {
	char reply[128];
	if (session == NULL || session->mode == 1) {
		copy_text(err, err_cap, "未连接，未点火");
		return -1;
	}
	if (session->mode == 0) {
		session->sim_flame = HS_FLAME_IGNITING;
		session->flame = HS_FLAME_IGNITING;
		copy_text(err, err_cap, "仿真已请求点火，火焰不是已着火");
		return 0;
	}
	if (exchange(session, "GCWS\tSTART_FID_IGNITE", NULL, reply, sizeof reply, 0) != 0) {
		session->flame = HS_FLAME_UNKNOWN;
		copy_text(err, err_cap, "点火命令未确认，火焰保持未知");
		return -1;
	}
	session->flame = HS_FLAME_UNKNOWN;
	copy_text(err, err_cap, "点火命令已应答，火焰是否点着仍未知");
	return 0;
}

int hs_session_valves(HsSession *session, int open, char *err, size_t err_cap) {
	char reply[128];
	const char *line = open ? "GCWS\tSTART_FID_VALVES" : "GCWS\tSTOP_FID_VALVES";
	if (session == NULL || session->mode == 1) {
		if (!open && session) {
			session->stop_unconfirmed = 1;
			copy_text(session->stop_text, sizeof session->stop_text, "关阀未确认");
		}
		copy_text(err, err_cap, open ? "未连接，未开阀" : "未连接，关阀未确认");
		return -1;
	}
	if (session->mode == 0) {
		copy_text(err, err_cap, open ? "仿真开阀（不是仪器确认）" : "仿真关阀（不是仪器确认）");
		return 0;
	}
	if (exchange(session, line, NULL, reply, sizeof reply, 0) != 0) {
		copy_text(err, err_cap, "气阀命令未确认");
		return -1;
	}
	copy_text(err, err_cap, "气阀命令已应答");
	return 0;
}

int hs_session_fid_auto(HsSession *session, int enable, char *err, size_t err_cap) {
	char reply[128];
	if (session == NULL || session->mode != 2 || !session->connected) {
		copy_text(err, err_cap, "未连接，未改自动点火");
		return -1;
	}
	if (exchange(session, enable ? "GCWS\tFID_AUTO_ON" : "GCWS\tFID_AUTO_OFF", NULL, reply, sizeof reply, 0) != 0) {
		copy_text(err, err_cap, "自动点火命令未确认");
		return -1;
	}
	copy_text(err, err_cap, "自动点火命令已应答，不代表火焰状态");
	return 0;
}

int hs_session_write_program(HsSession *session, const char *spec, char *err, size_t err_cap) {
	double rate[HS_OVEN_STAGES];
	double temp[HS_OVEN_STAGES];
	double hold[HS_OVEN_STAGES];
	double max_c = -1, eq = -1, stable = -1;
	int loop = -1;
	int rows = 0;
	char json[HS_JSON_MAX];
	char line[128];
	const char *p;
	char reply[128];
	if (session == NULL || spec == NULL) {
		return -1;
	}
	p = spec;
	while (*p && rows <= HS_OVEN_STAGES) {
		size_t n = 0;
		while (*p && *p != '\n' && n + 1 < sizeof line) {
			line[n++] = *p++;
		}
		line[n] = '\0';
		if (*p == '\n') {
			p++;
		}
		if (line[0] == '\0') {
			continue;
		}
		if (strncmp(line, "max=", 4) == 0) {
			max_c = strtod(line + 4, NULL);
		} else if (strncmp(line, "eq=", 3) == 0) {
			eq = strtod(line + 3, NULL);
		} else if (strncmp(line, "stable=", 7) == 0) {
			stable = strtod(line + 7, NULL);
		} else if (strncmp(line, "loop=", 5) == 0) {
			loop = atoi(line + 5);
		} else if (rows < HS_OVEN_STAGES) {
			if (sscanf(line, "%lf,%lf,%lf", &rate[rows], &temp[rows], &hold[rows]) != 3) {
				copy_text(err, err_cap, "温程行无法解析，未填 0");
				return -1;
			}
			rows++;
		}
	}
	if (rows != HS_OVEN_STAGES || max_c < 0 || eq < 0 || stable < 0 || loop < 0) {
		copy_text(err, err_cap, "温程不完整，缺少的阶未当成 0");
		return -1;
	}
	if (hs_oven_program_json(max_c, eq, stable, loop, rate, temp, hold, json, sizeof json) < 0) {
		copy_text(err, err_cap, "温程超出范围或超过 1536 字节");
		return -1;
	}
	if (session->mode != 2) {
		session->oven_set = temp[0];
		session->oven_known = 1;
		copy_text(err, err_cap, "温程仅保存在仿真/本地，未发送");
		return 0;
	}
	if (exchange(session, "GCWS\tWRITE_OVEN_PROGRAM", json, reply, sizeof reply, 0) != 0) {
		copy_text(err, err_cap, "温程写入未确认");
		return -1;
	}
	session->oven_set = temp[0];
	copy_text(err, err_cap, "温程已收到 WRITE_OK");
	return 0;
}

int hs_session_set_pid(HsSession *session, int which, double kp, double ki, double kd, char *err, size_t err_cap) {
	if (session == NULL || which < 0 || which > 2) {
		return -1;
	}
	if (!hs_pid_in_range(kp, ki, kd)) {
		copy_text(err, err_cap, "PID 必须在 0–100，空值和占位值都不会写入");
		return -1;
	}
	session->pid[which].known = 1;
	session->pid[which].kp = kp;
	session->pid[which].ki = ki;
	session->pid[which].kd = kd;
	copy_text(err, err_cap, "PID 已记在表里，尚未发送");
	return 0;
}

int hs_session_start_device_acq(HsSession *session, char *err, size_t err_cap) {
	uint8_t payload[2];
	uint8_t frame[32];
	int n;
	if (session == NULL || session->mode == 1) {
		copy_text(err, err_cap, "未连接，未开始设备采集");
		return -1;
	}
	if (session->flame == HS_FLAME_OUT || session->flame == HS_FLAME_UNKNOWN || session->flame == HS_FLAME_FAIL) {
		copy_text(session->alarm, sizeof session->alarm, "火焰不是已着火，按既定规则不拦截采集");
	}
	if (session->mode == 0) {
		session->device_acq = 1;
		session->acq_mark_ms = 0;
		copy_text(err, err_cap, "仿真设备采集已开始，未联网");
		return 0;
	}
	if (hs_acq_encode_start(HS_DEVICE_ACQ_REQUEST_MS, payload, sizeof payload) < 0) {
		return -1;
	}
	n = hs_frame_encode(0x01, 1, payload, 2, frame, sizeof frame);
	if (n < 0 || transport_write(session, frame, n) != 0) {
		copy_text(err, err_cap, "START_ACQ 未确认");
		return -1;
	}
	session->device_acq = 1;
	copy_text(err, err_cap, "已请求设备采集。间隔沿用现有工作站的 100 ms 请求，不是已核实采样率");
	return 0;
}

int hs_session_stop_device_acq(HsSession *session, char *err, size_t err_cap) {
	uint8_t frame[32];
	int n;
	if (session == NULL) {
		return -1;
	}
	if (session->mode != 2 || !session->connected) {
		session->device_acq = 0;
		if (session->mode == 0) {
			copy_text(err, err_cap, "仿真设备采集已停");
			return 0;
		}
		session->stop_unconfirmed = 1;
		copy_text(err, err_cap, "设备采集停止未确认");
		return -1;
	}
	n = hs_frame_encode(0x02, 1, NULL, 0, frame, sizeof frame);
	if (n < 0 || transport_write(session, frame, n) != 0) {
		session->stop_unconfirmed = 1;
		copy_text(err, err_cap, "STOP_ACQ 未确认");
		return -1;
	}
	session->device_acq = 0;
	copy_text(err, err_cap, "已请求停止设备采集。这不是工作站谱图 data3=2，也不是停热");
	return 0;
}

void hs_session_set_sim_flame(HsSession *session, HsFlame flame) {
	if (session) {
		session->sim_flame = flame;
		if (session->mode == 0) {
			session->flame = flame;
		}
	}
}

void hs_session_set_sim_pressure(HsSession *session, double h2, double air, int known) {
	if (session == NULL) {
		return;
	}
	session->pressure_known = known;
	session->h2 = h2;
	session->air = air;
	session->h2_kind = known ? HS_PRESSURE_NAMED_MPA : HS_PRESSURE_ABSENT;
	session->air_kind = known ? HS_PRESSURE_NAMED_MPA : HS_PRESSURE_ABSENT;
}

void hs_session_set_sim_period_ms(HsSession *session, int period_ms) {
	if (session && period_ms > 0) {
		session->sim_period = period_ms;
	}
}

void hs_session_arm_linear_pa(HsSession *session, int armed) {
	if (session) {
		session->linear_armed = armed ? 1 : 0;
	}
}

int hs_session_linear_pa_armed(const HsSession *session) {
	return session && session->linear_armed;
}

int hs_session_push_sample(HsSession *session, const HsSample *sample) {
	HsSample copy;
	if (session == NULL || sample == NULL) {
		return -1;
	}
	copy = *sample;
	if (copy.unit == HS_UNIT_UNKNOWN && session->linear_armed) {
		copy.unit = HS_UNIT_LINEAR_PA;
	}
	return hs_stream_push(session->stream, &copy);
}

static int hw_command(HsSession *session, int32_t data1, int32_t data2, int32_t data3, HsTriple *sent, char *err, size_t err_cap, const char *status) {
	if (hs_hw_export_blocked(0)) {
		copy_text(err, err_cap, "导出被阻止");
		return -1;
	}
	if (sent) {
		sent->data1 = data1;
		sent->data2 = data2;
		sent->data3 = data3;
	}
	copy_text(session->hw_status, sizeof session->hw_status, status);
	copy_text(err, err_cap, status);
	return 0;
}

int hs_session_hw_start(HsSession *session, HsTriple *sent_or_null, char *err, size_t err_cap) {
	char why[200];
	if (session == NULL) {
		return -1;
	}
	if (!hs_hw_interval_matches(hs_stream_period_ms(session->stream), session->hw_interval_ms, session->hw_interval_ok, why, sizeof why)) {
		copy_text(session->hw_status, sizeof session->hw_status, why);
		copy_text(err, err_cap, why);
		return -1;
	}
	return hw_command(session, 1, 0, 1, sent_or_null, err, err_cap, "已组 data3=1。DLL 返回 1 也只表示窗口消息已发出");
}

int hs_session_hw_stop(HsSession *session, char *err, size_t err_cap) {
	HsTriple triple;
	if (session == NULL) {
		return -1;
	}
	return hw_command(session, 1, 1, 2, &triple, err, err_cap, "data3=2 只停止工作站取数，不停热、不关气");
}

int hs_session_hw_zero(HsSession *session, char *err, size_t err_cap) {
	HsTriple triple;
	if (session == NULL) {
		return -1;
	}
	return hw_command(session, 1, 0, 5, &triple, err, err_cap, "data3=5 归零只作用于工作站通道");
}

int hs_session_poll_hw_sample(HsSession *session, HsTriple *out, char *note, size_t note_cap) {
	int32_t a = 0, b = 0, c = 0;
	int rc;
	if (session == NULL || out == NULL) {
		return -1;
	}
	rc = hs_stream_next_hw_triple(session->stream, session->hw_interval_ms, session->hw_interval_ok, &a, &b, &c, note, note_cap);
	if (rc == 1) {
		out->data1 = a;
		out->data2 = b;
		out->data3 = c;
		copy_text(session->hw_status, sizeof session->hw_status, note && note[0] ? note : "有待投递的点");
	} else if (note && note[0]) {
		copy_text(session->hw_status, sizeof session->hw_status, note);
	}
	return rc;
}

int hs_session_note_hwsys(HsSession *session, const char *path, int refreshed, int timed_out) {
	if (session == NULL) {
		return -1;
	}
	if (path == NULL || path[0] == '\0') {
		copy_text(session->hw_status, sizeof session->hw_status, "未配置工作站目录，不能查看 hwsys.dll 时间");
		return -1;
	}
	if (timed_out || !refreshed) {
		copy_text(session->hw_status, sizeof session->hw_status, "hwsys.dll 修改时间未刷新。原文称工作站忙时会这样。启动未确认");
		return -1;
	}
	copy_text(session->hw_status, sizeof session->hw_status, "hwsys.dll 时间已刷新。这不代表采集成功，也未与 HW-2000GMP 联调");
	return 0;
}

int hs_session_write_report(HsSession *session, const char *path, int64_t now_ms) {
	FILE *fp;
	HsSnapshot snap;
	if (session == NULL || path == NULL) {
		return -1;
	}
	hs_session_snapshot(session, now_ms, &snap);
	fp = fopen(path, "wb");
	if (!fp) {
		return -1;
	}
	fprintf(fp, "仪器: 自有色谱仪（控制协议 GCWS）\n");
	fprintf(fp, "连接: %s\n", snap.link);
	fprintf(fp, "氢气: %s\n空气: %s\n", snap.h2, snap.air);
	fprintf(fp, "火焰: %s\nFID电流: %s\n", snap.flame, snap.fid_pa);
	fprintf(fp, "进样口目标/当前: %s / %s\n", snap.inlet_set, snap.inlet_act);
	fprintf(fp, "检测器目标/当前: %s / %s\n", snap.det_set, snap.det_act);
	fprintf(fp, "柱箱目标/当前: %s / %s\n", snap.oven_set, snap.oven_act);
	fprintf(fp, "采样: %s\n%s\n", snap.unit_note, snap.pace);
	fprintf(fp, "说明: SDK 返回 1 不是采集成功。HW-2000 与 HW-2000GMP 是否兼容，本次没有实机结果。\n");
	fclose(fp);
	return 0;
}

int hs_session_enroll(HsSession *session, const char *password, const char *confirm, int64_t now_ms, char *err, size_t err_cap) {
	int rc;
	if (session == NULL) {
		return -1;
	}
	rc = hs_auth_enroll(session->auth, password, confirm, now_ms, HS_AUTH_ITERS);
	if (rc != 0) {
		copy_text(err, err_cap, hs_auth_status(session->auth, now_ms));
		return -1;
	}
	copy_text(err, err_cap, "维护口令已设置");
	return 0;
}

int hs_session_verify(HsSession *session, const char *password, int64_t now_ms, char *err, size_t err_cap) {
	int rc = hs_auth_verify(session->auth, password, now_ms);
	copy_text(err, err_cap, hs_auth_status(session->auth, now_ms));
	return rc == 0 ? 0 : -1;
}

void hs_session_lock_engineer(HsSession *session, int64_t now_ms) {
	if (session) {
		hs_auth_lock(session->auth, now_ms);
	}
}

int hs_session_save_column_note(HsSession *session, const char *text, int64_t now_ms, char *err, size_t err_cap) {
	char path[560];
	FILE *fp;
	if (session == NULL || !hs_auth_allows(session->auth, now_ms)) {
		copy_text(err, err_cap, "维护未解锁，色谱柱备注未保存");
		return -1;
	}
	snprintf(path, sizeof path, "%s/column-note.txt", session->dir);
	fp = fopen(path, "wb");
	if (!fp) {
		copy_text(err, err_cap, "备注无法写入");
		return -1;
	}
	fputs(text ? text : "", fp);
	fclose(fp);
	copy_text(err, err_cap, "备注只在本机，没有发给仪器");
	return 0;
}

int hs_session_log_bytes(const HsSession *session) {
	return session ? session->log_bytes : 0;
}

void hs_session_trace(HsSession *session, const char *line) {
	if (session) {
		log_line(session, line);
	}
}

const char *hs_session_written(const HsSession *session) {
	return session ? session->written : "";
}

const char *hs_session_recovery_banner(HsSession *session) {
	return session ? session->banner : "";
}

int hs_session_observe_aux(HsSession *session, int64_t now_ms, int inlet_fresh, double inlet_act, int det_fresh, double det_act, int oven_fault) {
	char reply[128];
	int inlet_band;
	int det_band;
	if (session == NULL) {
		return -1;
	}
	if (oven_fault > 0) {
		session->oven_fault = 1;
		session->oven_wait_cancelled = 1;
		push_event(session, "柱箱故障，取消等待中的柱箱启动");
		return 0;
	}
	if (oven_fault == 0) {
		session->oven_fault = 0;
	}
	if (!session->heat_running || session->oven_wait_cancelled || !session->want_oven || session->oven_started) {
		return 0;
	}
	if (inlet_fresh) {
		session->inlet_act = inlet_act;
		session->inlet_known = 1;
		session->temp_ms = now_ms;
		inlet_band = fabs(inlet_act - session->inlet_set) <= HS_AUX_STABLE_BAND_C;
		if (inlet_band) {
			session->inlet_stable_polls++;
		} else {
			session->inlet_stable_polls = 0;
		}
	}
	if (det_fresh) {
		session->det_act = det_act;
		session->det_known = 1;
		session->temp_ms = now_ms;
		det_band = fabs(det_act - session->det_set) <= HS_AUX_STABLE_BAND_C;
		if (det_band) {
			session->det_stable_polls++;
		} else {
			session->det_stable_polls = 0;
		}
	}
	if ((session->want_inlet && session->inlet_stable_polls < HS_AUX_STABLE_POLLS) || (session->want_det && session->det_stable_polls < HS_AUX_STABLE_POLLS)) {
		return 0;
	}
	if (!session->want_inlet && !session->want_det) {
		return 0;
	}
	if (session->mode != 2) {
		session->oven_started = 1;
		push_event(session, "仿真柱箱在连续 10 次稳温后启动");
		return 1;
	}
	if (exchange(session, "GCWS\tSTART_OVEN_CONTROL", NULL, reply, sizeof reply, now_ms) != 0) {
		session->oven_wait_cancelled = 1;
		push_event(session, "柱箱启动未确认，不再补发");
		return -1;
	}
	session->oven_started = 1;
	push_event(session, "柱箱在连续 10 次新鲜稳温后收到 WRITE_OK");
	return 1;
}

int hs_session_set_program(HsSession *session, const HsProgram *program, char *err, size_t err_cap) {
	if (session == NULL || program == NULL || hs_program_check(program, err, err_cap) != 0) {
		return -1;
	}
	session->program = *program;
	session->program.known = 1;
	session->oven_set = program->temperature[0];
	session->oven_known = 1;
	copy_text(err, err_cap, "温程已接受，尚未下载");
	return 0;
}

void hs_session_get_program(const HsSession *session, HsProgram *program) {
	if (program == NULL) {
		return;
	}
	if (session == NULL) {
		memset(program, 0, sizeof *program);
		return;
	}
	*program = session->program;
}

int hs_session_read_program(HsSession *session, char *err, size_t err_cap) {
	char reply[HS_JSON_MAX];
	HsProgram program;
	if (session == NULL || !live_ready(session)) {
		copy_text(err, err_cap, "未连接，温程未读");
		return -1;
	}
	if (exchange(session, "GCWS\tREAD_OVEN_PROGRAM", NULL, reply, sizeof reply, 0) != 0) {
		copy_text(err, err_cap, "读取温程超时或无应答");
		return -1;
	}
	if (hs_program_from_json(reply, &program, err, err_cap) != 0) {
		return -1;
	}
	session->program = program;
	session->oven_set = program.temperature[0];
	copy_text(err, err_cap, "已从设备读取温程");
	return 0;
}

int hs_session_write_program_now(HsSession *session, char *err, size_t err_cap) {
	char json[HS_JSON_MAX];
	char reply[128];
	if (session == NULL || !session->program.known) {
		copy_text(err, err_cap, "没有已校验的温程，拒绝下载");
		return -1;
	}
	if (hs_program_check(&session->program, err, err_cap) != 0) {
		return -1;
	}
	if (hs_oven_program_json(session->program.max_c, session->program.eq_min, session->program.stable_sec, session->program.loop, session->program.rate, session->program.temperature, session->program.hold, json, sizeof json) < 0) {
		copy_text(err, err_cap, "温程无法编码");
		return -1;
	}
	if (session->mode != 2) {
		copy_text(err, err_cap, "温程只保存在本地，当前没有在线连接");
		return 0;
	}
	if (exchange(session, "GCWS\tWRITE_OVEN_PROGRAM", json, reply, sizeof reply, 0) != 0) {
		copy_text(err, err_cap, "温程下载没有 WRITE_OK");
		return -1;
	}
	copy_text(err, err_cap, "温程下载已收到 WRITE_OK");
	return 0;
}

void hs_session_get_pid(const HsSession *session, int which, HsPidRow *row) {
	if (row == NULL) {
		return;
	}
	memset(row, 0, sizeof *row);
	if (session && which >= 0 && which < 3) {
		*row = session->pid[which];
	}
}

int hs_session_read_pids(HsSession *session, char *err, size_t err_cap) {
	const char *lines[] = {"GCWS\tREAD_OVEN_PID", "GCWS\tREAD_INLET_PID", "GCWS\tREAD_DETECTOR_PID"};
	int i;
	if (session == NULL || !live_ready(session)) {
		copy_text(err, err_cap, "未连接，PID 未读，表保持未读取");
		return -1;
	}
	for (i = 0; i < 3; i++) {
		char reply[256];
		int ok_p = 0;
		int ok_i = 0;
		int ok_d = 0;
		double kp;
		double ki;
		double kd;
		if (exchange(session, lines[i], NULL, reply, sizeof reply, 0) != 0) {
			session->pid[i].known = 0;
			copy_text(err, err_cap, "PID 读取超时，未把缺测填成 0");
			return -1;
		}
		kp = hs_json_number(reply, "kp", &ok_p);
		ki = hs_json_number(reply, "ki", &ok_i);
		kd = hs_json_number(reply, "kd", &ok_d);
		if (!ok_p || !ok_i || !ok_d || !hs_pid_in_range(kp, ki, kd)) {
			session->pid[i].known = 0;
			copy_text(err, err_cap, "PID 报文无效，该路保持未读取");
			return -1;
		}
		session->pid[i].known = 1;
		session->pid[i].kp = kp;
		session->pid[i].ki = ki;
		session->pid[i].kd = kd;
	}
	copy_text(err, err_cap, "三路 PID 已读取");
	return 0;
}

void hs_session_pid_confirm_text(const HsSession *session, char *out, size_t cap) {
	if (out == NULL || cap == 0) {
		return;
	}
	if (session == NULL || !session->pid[0].known || !session->pid[1].known || !session->pid[2].known) {
		out[0] = '\0';
		return;
	}
	snprintf(out, cap, "确认写入 PID 柱箱 %.4g/%.4g/%.4g 进样口 %.4g/%.4g/%.4g 检测器 %.4g/%.4g/%.4g", session->pid[0].kp, session->pid[0].ki, session->pid[0].kd, session->pid[1].kp, session->pid[1].ki, session->pid[1].kd, session->pid[2].kp, session->pid[2].ki, session->pid[2].kd);
}

int hs_session_write_pids_confirmed(HsSession *session, const char *confirm, int64_t now_ms, char *err, size_t err_cap) {
	char expected[240];
	char json[256];
	char reply[128];
	const char *lines[] = {"GCWS\tWRITE_OVEN_PID", "GCWS\tWRITE_INLET_PID", "GCWS\tWRITE_DETECTOR_PID"};
	int i;
	if (session == NULL) {
		return -1;
	}
	if (!hs_auth_allows(session->auth, now_ms)) {
		copy_text(err, err_cap, "维护未解锁，PID 未发送");
		return -1;
	}
	hs_session_pid_confirm_text(session, expected, sizeof expected);
	if (expected[0] == '\0' || confirm == NULL || strcmp(confirm, expected) != 0) {
		copy_text(err, err_cap, "PID 未确认目标数值，拒绝发送");
		return -1;
	}
	if (session->mode != 2) {
		copy_text(err, err_cap, "当前不是在线连接，PID 未发送");
		return -1;
	}
	for (i = 0; i < 3; i++) {
		if (hs_pid_json(session->pid[i].kp, session->pid[i].ki, session->pid[i].kd, json, sizeof json) < 0) {
			copy_text(err, err_cap, "PID 数值无效");
			return -1;
		}
		if (exchange(session, lines[i], json, reply, sizeof reply, now_ms) != 0) {
			copy_text(err, err_cap, "PID 写入没有 WRITE_OK，已停止后续路");
			return -1;
		}
	}
	hs_auth_touch(session->auth, now_ms);
	copy_text(err, err_cap, "三路 PID 均收到 WRITE_OK");
	return 0;
}

int hs_session_set_hw_interval(HsSession *session, int interval_ms, int confirmed, char *err, size_t err_cap) {
	if (session == NULL || !confirmed || interval_ms < 20 || interval_ms > 10000) {
		copy_text(err, err_cap, "必须勾选已核实，并填写 20–10000 ms。不会写 HWFrequence.txt");
		return -1;
	}
	session->hw_interval_ms = interval_ms;
	session->hw_interval_ok = 1;
	copy_text(err, err_cap, "已记录操作者核实的工作站间隔。HWFrequence.txt 语法未知，未写文件");
	return 0;
}

int hs_session_probe(HsSession *session, const char *host, int port, char *err, size_t err_cap) {
	HsLive *live = NULL;
	char reply[128];
	const char *hello = "GCWS\tHELLO\tPROBE\r\n";
	int n;
	if (session == NULL || !hs_host_port_ok(host, port, err, err_cap)) {
		return -1;
	}
	if (hs_live_connect(&live, host, port, 2000, err, err_cap) != 0) {
		return -1;
	}
	n = hs_live_write(live, hello, (int)strlen(hello));
	session->net_writes += n > 0 ? n : 0;
	if (n != (int)strlen(hello) || hs_live_read(live, reply, (int)sizeof reply - 1, 2000) < 0 || strstr(reply, "HELLO_OK") == NULL) {
		hs_live_close(live);
		copy_text(err, err_cap, "探测没有 HELLO_OK，连接已关闭，没有取得控制权");
		return -1;
	}
	hs_live_close(live);
	copy_text(err, err_cap, "探测收到 HELLO_OK。这是 PROBE，不占用 PC 写入权，连接已关闭");
	push_event(session, "手动探测地址收到 HELLO_OK");
	return 0;
}

int hs_session_ingest_pressure_json(HsSession *session, const char *json, int64_t now_ms) {
	if (session == NULL) {
		return -1;
	}
	return apply_pressure_json(session, json, now_ms);
}

int hs_session_write_setpoints(HsSession *session, double inlet_c, double detector_c, char *err, size_t err_cap) {
	char json[128];
	char reply[128];
	if (session == NULL || !live_ready(session)) {
		copy_text(err, err_cap, "未连接，温度设定未发送");
		return -1;
	}
	if (!hs_setpoint_ok(inlet_c) || !hs_setpoint_ok(detector_c)) {
		copy_text(err, err_cap, "设定温度无效或超过 450 °C");
		return -1;
	}
	snprintf(json, sizeof json, "{\n  \"setpoint\": \"%.0f\"\n}\n", inlet_c);
	if (exchange(session, "GCWS\tWRITE_INLET_TEMP", json, reply, sizeof reply, 0) != 0) {
		copy_text(err, err_cap, "进样口设定没有 WRITE_OK");
		return -1;
	}
	snprintf(json, sizeof json, "{\n  \"setpoint\": \"%.0f\"\n}\n", detector_c);
	if (exchange(session, "GCWS\tWRITE_DETECTOR_TEMP", json, reply, sizeof reply, 0) != 0) {
		copy_text(err, err_cap, "检测器设定没有 WRITE_OK");
		return -1;
	}
	session->inlet_set = inlet_c;
	session->det_set = detector_c;
	copy_text(err, err_cap, "进样口和检测器设定已收到 WRITE_OK");
	return 0;
}

int hs_session_device_apply_blocked(char *err, size_t err_cap) {
	copy_text(err, err_cap, "这些数字只存在于本工作站文件，不会下发。本页不读取、不修改设备里的跳闸阈值。");
	return -1;
}

void hs_session_event_text(const HsSession *session, char *out, size_t cap) {
	int i;
	size_t used = 0;
	if (out == NULL || cap == 0) {
		return;
	}
	out[0] = '\0';
	if (session == NULL) {
		return;
	}
	for (i = 0; i < session->event_n && used + 2 < cap; i++) {
		int n = snprintf(out + used, cap - used, "%s\n", session->events[i]);
		if (n < 0) {
			break;
		}
		used += (size_t)n;
	}
}

void hs_session_stopwatch_text(HsSession *session, int64_t now_ms, char *out, size_t cap) {
	long long elapsed;
	if (out == NULL || cap == 0) {
		return;
	}
	if (session == NULL || !session->device_acq || session->stopwatch_hold) {
		snprintf(out, cap, "00:00");
		return;
	}
	if (session->acq_mark_ms == 0) {
		session->acq_mark_ms = now_ms;
	}
	elapsed = now_ms - session->acq_mark_ms;
	if (elapsed < 0) {
		elapsed = 0;
	}
	snprintf(out, cap, "%02lld:%02lld", elapsed / 60000, (elapsed / 1000) % 60);
}

void hs_session_reset_stopwatch(HsSession *session) {
	if (session) {
		session->stopwatch_hold = 1;
	}
}

int hs_session_trace_count(const HsSession *session) {
	return session ? session->trace_n : 0;
}

int hs_session_trace_at(const HsSession *session, int index, double *inlet, double *detector, double *oven) {
	if (session == NULL || index < 0 || index >= session->trace_n) {
		return -1;
	}
	if (inlet) {
		*inlet = session->trace_inlet[index];
	}
	if (detector) {
		*detector = session->trace_det[index];
	}
	if (oven) {
		*oven = session->trace_oven[index];
	}
	return 0;
}

int hs_utf8_to_wide(const char *utf8, wchar_t *wide, size_t wide_cap) {
	size_t n = 0;
	const unsigned char *p = (const unsigned char *)utf8;
	if (utf8 == NULL || wide == NULL || wide_cap == 0) {
		return -1;
	}
	while (*p && n + 1 < wide_cap) {
		unsigned code;
		if (*p < 0x80) {
			code = *p++;
		} else if ((*p & 0xE0) == 0xC0 && p[1]) {
			code = ((p[0] & 0x1F) << 6) | (p[1] & 0x3F);
			p += 2;
		} else if ((*p & 0xF0) == 0xE0 && p[1] && p[2]) {
			code = ((p[0] & 0x0F) << 12) | ((p[1] & 0x3F) << 6) | (p[2] & 0x3F);
			p += 3;
		} else {
			return -1;
		}
		wide[n++] = (wchar_t)code;
	}
	wide[n] = 0;
	return (int)n;
}
