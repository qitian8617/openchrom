#include "hs.h"

#include <ctype.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static int verb_eq(const char *verb, const char *name) {
	return strcmp(verb, name) == 0;
}

const char *hs_command_verb(const char *line) {
	static char safe[41];
	const char *token;
	int tab;
	int i;
	int n = 0;
	if (line == NULL) {
		return "";
	}
	while (*line == ' ' || *line == '\t') {
		line++;
	}
	tab = -1;
	for (i = 0; line[i] != '\0'; i++) {
		if (line[i] == '\t') {
			tab = i;
			break;
		}
	}
	token = tab >= 0 ? line + tab + 1 : line;
	for (i = 0; token[i] != '\0' && token[i] != '\t' && token[i] != ' ' && n < 40; i++) {
		unsigned char c = (unsigned char)token[i];
		if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_') {
			safe[n++] = (char)c;
		} else if (c >= 'a' && c <= 'z') {
			safe[n++] = (char)(c - 32);
		}
	}
	safe[n] = '\0';
	return safe;
}

static int smuggled(const char *text) {
	const char *first;
	if (text == NULL) {
		return 0;
	}
	first = strstr(text, "GCWS");
	if (first == NULL) {
		const char *p = text;
		while (*p) {
			if ((p[0] == 'g' || p[0] == 'G') && (p[1] == 'c' || p[1] == 'C') && (p[2] == 'w' || p[2] == 'W') && (p[3] == 's' || p[3] == 'S')) {
				first = p;
				break;
			}
			p++;
		}
	}
	if (first == NULL) {
		return 0;
	}
	return strstr(first + 4, "GCWS") != NULL || strstr(first + 4, "gcws") != NULL;
}

HsCmdKind hs_classify_command(const char *line) {
	const char *verb;
	if (line == NULL || strchr(line, '\n') != NULL || strchr(line, '\r') != NULL || smuggled(line)) {
		return HS_CMD_UNSUPPORTED;
	}
	verb = hs_command_verb(line);
	if (verb_eq(verb, "HELLO") || verb_eq(verb, "READ_OVEN_PROGRAM") || verb_eq(verb, "READ_OVEN_PID") || verb_eq(verb, "READ_INLET_PID") || verb_eq(verb, "READ_DETECTOR_PID") || verb_eq(verb, "READ_OVEN_TEMP") || verb_eq(verb, "READ_INLET_TEMP") || verb_eq(verb, "READ_DETECTOR_TEMP") || verb_eq(verb, "READ_FID_STATUS") || verb_eq(verb, "READ_GAS_PRESSURE") || verb_eq(verb, "READ_CONTROL_STATUS")) {
		return HS_CMD_READ;
	}
	if (verb_eq(verb, "STOP_INLET_HEAT") || verb_eq(verb, "STOP_DETECTOR_HEAT") || verb_eq(verb, "STOP_OVEN_CONTROL") || verb_eq(verb, "STOP_FID_VALVES")) {
		return HS_CMD_SAFETY;
	}
	if (verb_eq(verb, "WRITE_OVEN_PROGRAM") || verb_eq(verb, "WRITE_INLET_TEMP") || verb_eq(verb, "WRITE_DETECTOR_TEMP") || verb_eq(verb, "WRITE_ZONE_SELECT") || verb_eq(verb, "START_INLET_HEAT") || verb_eq(verb, "START_DETECTOR_HEAT") || verb_eq(verb, "START_OVEN_CONTROL") || verb_eq(verb, "START_FID_IGNITE") || verb_eq(verb, "START_FID_VALVES") || verb_eq(verb, "FID_AUTO_ON") || verb_eq(verb, "FID_AUTO_OFF")) {
		return HS_CMD_OPERATOR;
	}
	if (verb_eq(verb, "WRITE_OVEN_PID") || verb_eq(verb, "WRITE_INLET_PID") || verb_eq(verb, "WRITE_DETECTOR_PID")) {
		return HS_CMD_ENGINEERING;
	}
	return HS_CMD_UNSUPPORTED;
}

int hs_command_allowed_when_locked(const char *line) {
	HsCmdKind kind = hs_classify_command(line);
	return kind == HS_CMD_READ || kind == HS_CMD_SAFETY || kind == HS_CMD_OPERATOR;
}

int hs_payload_ok(const char *payload) {
	if (payload == NULL) {
		return 1;
	}
	if (strstr(payload, "GCWS") != NULL || strstr(payload, "gcws") != NULL) {
		return 0;
	}
	return 1;
}

int hs_binary_opcode_ok(unsigned command) {
	switch (command) {
	case 0x01:
	case 0x02:
	case 0x03:
	case 0x10:
	case 0x11:
	case 0x20:
	case 0x21:
	case 0x7F:
	case 0x80:
	case 0x81:
		return 1;
	default:
		return 0;
	}
}

HsHeatGate hs_heat_start_gate(int h2_known, double h2_mpa, int air_known, double air_mpa, int64_t sample_ms, int64_t now_ms) {
	int64_t age;
	if (!h2_known || !air_known) {
		return HS_HEAT_BLOCK_UNKNOWN;
	}
	if (!isfinite(h2_mpa) || !isfinite(air_mpa)) {
		return HS_HEAT_BLOCK_NON_FINITE;
	}
	age = now_ms - sample_ms;
	if (age < 0 || age > HS_PRESSURE_FRESH_MS) {
		return HS_HEAT_BLOCK_STALE;
	}
	if (h2_mpa < HS_PRESSURE_MIN_MPA || air_mpa < HS_PRESSURE_MIN_MPA) {
		return HS_HEAT_BLOCK_BELOW;
	}
	return HS_HEAT_ALLOW;
}

const char *hs_heat_gate_text(HsHeatGate gate) {
	switch (gate) {
	case HS_HEAT_ALLOW:
		return "两路气压新鲜且不低于 0.1 MPa";
	case HS_HEAT_BLOCK_UNKNOWN:
		return "气压未知，拒绝启动温控";
	case HS_HEAT_BLOCK_STALE:
		return "气压过期，拒绝启动温控";
	case HS_HEAT_BLOCK_NON_FINITE:
		return "气压读数无效，拒绝启动温控";
	case HS_HEAT_BLOCK_BELOW:
		return "氢气或空气低于 0.1 MPa（气压门槛，不是气流量），拒绝启动温控";
	default:
		return "温控启动被拒绝";
	}
}

static int key_span(const char *json, const char *key, const char **value) {
	char pattern[80];
	const char *found;
	const char *p;
	if (json == NULL || key == NULL || strlen(key) > 60) {
		return 0;
	}
	snprintf(pattern, sizeof pattern, "\"%s\"", key);
	found = strstr(json, pattern);
	if (found == NULL) {
		return 0;
	}
	p = found + strlen(pattern);
	while (*p == ' ' || *p == '\t' || *p == '\n' || *p == '\r') {
		p++;
	}
	if (*p != ':') {
		return 0;
	}
	p++;
	while (*p == ' ' || *p == '\t' || *p == '\n' || *p == '\r') {
		p++;
	}
	*value = p;
	return 1;
}

int hs_json_has(const char *json, const char *key) {
	const char *value;
	return key_span(json, key, &value);
}

int hs_json_quoted(const char *json, const char *key, char *out, size_t cap) {
	const char *p;
	size_t n = 0;
	if (out == NULL || cap == 0 || !key_span(json, key, &p) || *p != '"') {
		return 0;
	}
	p++;
	while (*p != '\0' && *p != '"') {
		if (n + 1 >= cap) {
			return 0;
		}
		out[n++] = *p++;
	}
	if (*p != '"') {
		return 0;
	}
	out[n] = '\0';
	return 1;
}

double hs_json_number(const char *json, const char *key, int *ok) {
	const char *p;
	char quoted[64];
	char *end = NULL;
	double value;
	if (ok) {
		*ok = 0;
	}
	if (!key_span(json, key, &p)) {
		return 0.0;
	}
	if (*p == '"') {
		if (!hs_json_quoted(json, key, quoted, sizeof quoted)) {
			return 0.0;
		}
		value = strtod(quoted, &end);
		if (end == quoted || *end != '\0' || !isfinite(value)) {
			return 0.0;
		}
	} else {
		value = strtod(p, &end);
		if (end == p || !isfinite(value)) {
			return 0.0;
		}
	}
	if (ok) {
		*ok = 1;
	}
	return value;
}

int hs_format_number(double value, char *out, size_t cap) {
	char buf[64];
	if (out == NULL || cap == 0 || !isfinite(value)) {
		return -1;
	}
	snprintf(buf, sizeof buf, "%.6f", value);
	{
		char *dot = strchr(buf, '.');
		if (dot) {
			char *end = buf + strlen(buf);
			while (end > dot + 1 && end[-1] == '0') {
				*--end = '\0';
			}
			if (end > dot && end[-1] == '.') {
				end[-1] = '\0';
			}
		}
	}
	if (strlen(buf) + 1 > cap) {
		return -1;
	}
	memcpy(out, buf, strlen(buf) + 1);
	return 0;
}

int hs_setpoint_ok(double celsius) {
	return isfinite(celsius) && celsius >= 0.0 && celsius <= HS_TEMP_CEILING_C;
}

int hs_pid_json(double kp, double ki, double kd, char *out, size_t cap) {
	char a[32], b[32], c[32];
	int n;
	if (!isfinite(kp) || !isfinite(ki) || !isfinite(kd) || fabs(kp) > 1.0e6 || fabs(ki) > 1.0e6 || fabs(kd) > 1.0e6) {
		return -1;
	}
	if (hs_format_number(kp, a, sizeof a) || hs_format_number(ki, b, sizeof b) || hs_format_number(kd, c, sizeof c)) {
		return -1;
	}
	n = snprintf(out, cap, "{\n  \"kp\": \"%s\",\n  \"ki\": \"%s\",\n  \"kd\": \"%s\"\n}\n", a, b, c);
	if (n < 0 || (size_t)n >= cap) {
		return -1;
	}
	return n;
}

int hs_oven_program_json(double max_c, double eq_min, double stable_sec, int loop, const double *rate, const double *temp, const double *hold, char *out, size_t cap) {
	char body[HS_JSON_MAX];
	int n = 0;
	int i;
	char num[32];
	if (out == NULL || rate == NULL || temp == NULL || hold == NULL) {
		return -1;
	}
	if (!hs_setpoint_ok(max_c) || !isfinite(eq_min) || eq_min < 0 || !isfinite(stable_sec) || stable_sec < 0 || (loop != 0 && loop != 1)) {
		return -1;
	}
	if (hs_format_number(max_c, num, sizeof num)) {
		return -1;
	}
	n += snprintf(body + n, sizeof body - (size_t)n, "{\n  \"maxTemperature\": \"%s\",\n", num);
	if (hs_format_number(eq_min, num, sizeof num)) {
		return -1;
	}
	n += snprintf(body + n, sizeof body - (size_t)n, "  \"equilibriumTime\": \"%s\",\n", num);
	if (hs_format_number(stable_sec, num, sizeof num)) {
		return -1;
	}
	n += snprintf(body + n, sizeof body - (size_t)n, "  \"equilibriumStableSec\": \"%s\",\n  \"loop\": \"%d\",\n  \"rows\": [\n", num, loop);
	for (i = 0; i < HS_OVEN_STAGES; i++) {
		char r[32], t[32], h[32];
		if (!isfinite(rate[i]) || rate[i] < 0 || !hs_setpoint_ok(temp[i]) || !isfinite(hold[i]) || hold[i] < 0) {
			return -1;
		}
		if (hs_format_number(rate[i], r, sizeof r) || hs_format_number(temp[i], t, sizeof t) || hs_format_number(hold[i], h, sizeof h)) {
			return -1;
		}
		n += snprintf(body + n, sizeof body - (size_t)n, "%s    {\n      \"row\": %d,\n      \"rate\": \"%s\",\n      \"temperature\": \"%s\",\n      \"holdTime\": \"%s\"\n    }", i ? ",\n" : "", i, r, t, h);
		if (n < 0 || n >= (int)sizeof body) {
			return -1;
		}
	}
	n += snprintf(body + n, sizeof body - (size_t)n, "\n  ]\n}\n");
	if (n < 0 || n >= (int)sizeof body || n >= HS_JSON_MAX) {
		return -1;
	}
	if ((size_t)n >= cap) {
		return -1;
	}
	memcpy(out, body, (size_t)n + 1);
	return n;
}

HsFlame hs_flame_from_status(int known, int online, int busy, int flame, const char *state, int64_t sample_ms, int64_t now_ms) {
	int64_t age;
	if (!known) {
		return HS_FLAME_UNKNOWN;
	}
	age = now_ms - sample_ms;
	if (age < 0 || age > HS_STATUS_FRESH_MS) {
		return HS_FLAME_STALE;
	}
	if (!online) {
		return HS_FLAME_OFFLINE;
	}
	if (busy || (state != NULL && strcmp(state, "igniting") == 0)) {
		return HS_FLAME_IGNITING;
	}
	if (state != NULL && strcmp(state, "fail") == 0) {
		return HS_FLAME_FAIL;
	}
	if (flame) {
		return HS_FLAME_ON;
	}
	return HS_FLAME_OUT;
}

const char *hs_flame_text(HsFlame flame) {
	switch (flame) {
	case HS_FLAME_UNKNOWN:
		return "未知";
	case HS_FLAME_STALE:
		return "过期";
	case HS_FLAME_OFFLINE:
		return "板离线";
	case HS_FLAME_IGNITING:
		return "点火中";
	case HS_FLAME_FAIL:
		return "点火未成功";
	case HS_FLAME_ON:
		return "已着火";
	case HS_FLAME_OUT:
		return "未着火";
	default:
		return "未知";
	}
}

int hs_crc16(const uint8_t *data, int offset, int length) {
	int crc = 0xFFFF;
	int index;
	for (index = offset; index < offset + length; index++) {
		int bit;
		crc ^= data[index];
		for (bit = 0; bit < 8; bit++) {
			if (crc & 0x0001) {
				crc = (crc >> 1) ^ 0xA001;
			} else {
				crc >>= 1;
			}
		}
	}
	return crc & 0xFFFF;
}

int hs_frame_encode(unsigned command, unsigned sequence, const uint8_t *payload, int payload_len, uint8_t *out, int out_cap) {
	int crc;
	int total;
	if (payload_len < 0 || payload_len > 4096 || out == NULL) {
		return -1;
	}
	total = 10 + payload_len;
	if (out_cap < total) {
		return -1;
	}
	out[0] = 0xAA;
	out[1] = 0x55;
	out[2] = (uint8_t)command;
	out[3] = (uint8_t)sequence;
	out[4] = (uint8_t)(payload_len & 0xFF);
	out[5] = (uint8_t)((payload_len >> 8) & 0xFF);
	if (payload_len > 0) {
		memcpy(out + 6, payload, (size_t)payload_len);
	}
	crc = hs_crc16(out, 2, 4 + payload_len);
	out[6 + payload_len] = (uint8_t)(crc & 0xFF);
	out[7 + payload_len] = (uint8_t)((crc >> 8) & 0xFF);
	out[8 + payload_len] = 0x55;
	out[9 + payload_len] = 0xAA;
	return total;
}

int hs_frame_decode(const uint8_t *frame, int frame_len, unsigned *command, unsigned *sequence, uint8_t *payload, int payload_cap, int *payload_len) {
	int len;
	int crc_expected;
	int crc_actual;
	if (frame == NULL || frame_len < 10) {
		return -1;
	}
	if (frame[0] != 0xAA || frame[1] != 0x55) {
		return -1;
	}
	len = frame[4] | (frame[5] << 8);
	if (frame_len != 10 + len) {
		return -1;
	}
	if (frame[8 + len] != 0x55 || frame[9 + len] != 0xAA) {
		return -1;
	}
	crc_expected = hs_crc16(frame, 2, 4 + len);
	crc_actual = frame[6 + len] | (frame[7 + len] << 8);
	if (crc_expected != crc_actual) {
		return -1;
	}
	if (payload_len) {
		*payload_len = len;
	}
	if (len > 0) {
		if (payload == NULL || payload_cap < len) {
			return -1;
		}
		memcpy(payload, frame + 6, (size_t)len);
	}
	if (command) {
		*command = frame[2];
	}
	if (sequence) {
		*sequence = frame[3];
	}
	return 0;
}

int hs_acq_encode_start(int interval_ms, uint8_t *out, int out_cap) {
	if (out == NULL || out_cap < 2 || interval_ms < 0 || interval_ms > 65535) {
		return -1;
	}
	out[0] = (uint8_t)(interval_ms & 0xFF);
	out[1] = (uint8_t)((interval_ms >> 8) & 0xFF);
	return 2;
}

static void put_u32(uint8_t *p, uint32_t v) {
	p[0] = (uint8_t)(v & 0xFF);
	p[1] = (uint8_t)((v >> 8) & 0xFF);
	p[2] = (uint8_t)((v >> 16) & 0xFF);
	p[3] = (uint8_t)((v >> 24) & 0xFF);
}

static uint32_t get_u32(const uint8_t *p) {
	return (uint32_t)p[0] | ((uint32_t)p[1] << 8) | ((uint32_t)p[2] << 16) | ((uint32_t)p[3] << 24);
}

int hs_acq_encode_batch(int batch_sequence, const int *times_ms, const float *signals, int count, uint8_t *out, int out_cap) {
	int need;
	int i;
	if (count < 0 || times_ms == NULL || signals == NULL || out == NULL) {
		return -1;
	}
	need = 6 + count * 8;
	if (out_cap < need) {
		return -1;
	}
	put_u32(out, (uint32_t)batch_sequence);
	out[4] = (uint8_t)(count & 0xFF);
	out[5] = (uint8_t)((count >> 8) & 0xFF);
	for (i = 0; i < count; i++) {
		uint32_t bits;
		float signal = signals[i];
		memcpy(&bits, &signal, 4);
		put_u32(out + 6 + i * 8, (uint32_t)(times_ms[i] < 0 ? 0 : times_ms[i]));
		put_u32(out + 10 + i * 8, bits);
	}
	return need;
}

int hs_acq_decode_batch(const uint8_t *payload, int payload_len, int *batch_sequence, int *times_ms, float *signals, int cap, int *count) {
	int n;
	int i;
	if (payload == NULL || payload_len < 6) {
		return -1;
	}
	n = payload[4] | (payload[5] << 8);
	if (payload_len < 6 + n * 8 || n > cap) {
		return -1;
	}
	if (batch_sequence) {
		*batch_sequence = (int)get_u32(payload);
	}
	for (i = 0; i < n; i++) {
		uint32_t bits = get_u32(payload + 10 + i * 8);
		float signal;
		memcpy(&signal, &bits, 4);
		if (times_ms) {
			times_ms[i] = (int)get_u32(payload + 6 + i * 8);
		}
		if (signals) {
			signals[i] = signal;
		}
	}
	if (count) {
		*count = n;
	}
	return 0;
}

void hs_hw_class_name(int index, char *out, size_t cap) {
	if (out == NULL || cap == 0) {
		return;
	}
	if (index <= 0) {
		snprintf(out, cap, "HWClass");
	} else {
		snprintf(out, cap, "HWClass%d", index);
	}
}

int hs_hw_export_blocked(int index) {
	return index >= 41 && index <= 50;
}

static int read_named_or_adc(const char *json, const char *named, const char *adc, double *out, HsPressureKind *kind) {
	int ok = 0;
	if (hs_json_has(json, named)) {
		*out = hs_json_number(json, named, &ok);
		*kind = ok ? HS_PRESSURE_NAMED_MPA : HS_PRESSURE_ABSENT;
		return ok;
	}
	if (hs_json_has(json, adc)) {
		*out = hs_json_number(json, adc, &ok);
		*kind = ok ? HS_PRESSURE_ADC_UNIT_UNCONFIRMED : HS_PRESSURE_ABSENT;
		return ok;
	}
	*kind = HS_PRESSURE_ABSENT;
	*out = 0.0;
	return 0;
}

int hs_parse_gas_pressure(const char *json, double *h2, double *air, HsPressureKind *h2_kind, HsPressureKind *air_kind) {
	HsPressureKind hk = HS_PRESSURE_ABSENT;
	HsPressureKind ak = HS_PRESSURE_ABSENT;
	double hv = 0.0;
	double av = 0.0;
	if (json == NULL) {
		return -1;
	}
	read_named_or_adc(json, "h2", "adc1", &hv, &hk);
	read_named_or_adc(json, "air", "adc2", &av, &ak);
	if (h2) {
		*h2 = hv;
	}
	if (air) {
		*air = av;
	}
	if (h2_kind) {
		*h2_kind = hk;
	}
	if (air_kind) {
		*air_kind = ak;
	}
	return 0;
}

static int stage_unused(double rate, double temp, double hold) {
	return rate <= 0.01 && hold <= 0.0 && fabs(temp) <= 0.01;
}

int hs_program_check(const HsProgram *program, char *err, size_t err_cap) {
	int i;
	double previous;
	int seen = 0;
	if (program == NULL) {
		return -1;
	}
	if (!hs_setpoint_ok(program->max_c) || program->eq_min < 0 || !isfinite(program->eq_min) || program->stable_sec < 0 || !isfinite(program->stable_sec) || (program->loop != 0 && program->loop != 1)) {
		if (err && err_cap) {
			snprintf(err, err_cap, "最高温度须在 0–450 °C，平衡时间和循环标志必须有效");
		}
		return -1;
	}
	previous = program->temperature[0];
	for (i = 0; i < HS_OVEN_STAGES; i++) {
		if (i > 0 && stage_unused(program->rate[i], program->temperature[i], program->hold[i])) {
			continue;
		}
		if (program->rate[i] < 0 || !isfinite(program->rate[i]) || program->hold[i] < 0 || !isfinite(program->hold[i]) || !hs_setpoint_ok(program->temperature[i]) || program->temperature[i] > program->max_c) {
			if (err && err_cap) {
				snprintf(err, err_cap, "第 %d 阶超出温度上限或速率/保持为负", i + 1);
			}
			return -1;
		}
		if (seen && program->rate[i] <= 0.01 && fabs(program->temperature[i] - previous) > 0.05) {
			if (err && err_cap) {
				snprintf(err, err_cap, "第 %d 阶速率为 0 却要求变温", i + 1);
			}
			return -1;
		}
		previous = program->temperature[i];
		seen = 1;
	}
	return 0;
}

long hs_program_duration_ms(const HsProgram *program) {
	double minutes;
	double temp;
	int i;
	if (program == NULL) {
		return -1;
	}
	minutes = program->hold[0] > 0 ? program->hold[0] : 0;
	temp = program->temperature[0];
	for (i = 1; i < HS_OVEN_STAGES; i++) {
		if (stage_unused(program->rate[i], program->temperature[i], program->hold[i])) {
			continue;
		}
		if (program->rate[i] > 0.01) {
			minutes += fabs(program->temperature[i] - temp) / program->rate[i];
		}
		if (program->hold[i] > 0) {
			minutes += program->hold[i];
		}
		temp = program->temperature[i];
	}
	if (minutes <= 0) {
		return 0;
	}
	return (long)llround(minutes * 60000.0);
}

int hs_program_from_json(const char *json, HsProgram *program, char *err, size_t err_cap) {
	int i;
	int ok = 0;
	const char *rows;
	const char *cursor;
	if (program == NULL || json == NULL) {
		return -1;
	}
	memset(program, 0, sizeof *program);
	if (!hs_json_has(json, "maxTemperature") || !hs_json_has(json, "rows")) {
		if (err && err_cap) {
			snprintf(err, err_cap, "温程报文缺少最高温度或阶梯，未把缺项当成 0");
		}
		return -1;
	}
	program->max_c = hs_json_number(json, "maxTemperature", &ok);
	if (!ok) {
		return -1;
	}
	program->eq_min = hs_json_has(json, "equilibriumTime") ? hs_json_number(json, "equilibriumTime", &ok) : 0;
	program->stable_sec = hs_json_has(json, "equilibriumStableSec") ? hs_json_number(json, "equilibriumStableSec", &ok) : 0;
	program->loop = 0;
	if (hs_json_has(json, "loop")) {
		double loop = hs_json_number(json, "loop", &ok);
		program->loop = ok && loop != 0.0;
	}
	rows = strstr(json, "\"rows\"");
	cursor = rows ? strchr(rows, '[') : NULL;
	if (cursor == NULL) {
		return -1;
	}
	cursor++;
	for (i = 0; i < HS_OVEN_STAGES; i++) {
		const char *rate = strstr(cursor, "\"rate\"");
		const char *temperature;
		const char *hold;
		const char *end;
		if (rate == NULL) {
			if (err && err_cap) {
				snprintf(err, err_cap, "温程不足 8 阶，未补 0");
			}
			return -1;
		}
		temperature = strstr(rate, "\"temperature\"");
		hold = temperature ? strstr(temperature, "\"holdTime\"") : NULL;
		end = hold ? strchr(hold, '}') : NULL;
		if (temperature == NULL || hold == NULL || end == NULL) {
			return -1;
		}
		program->rate[i] = hs_json_number(rate, "rate", &ok);
		program->temperature[i] = hs_json_number(temperature, "temperature", &ok);
		program->hold[i] = hs_json_number(hold, "holdTime", &ok);
		cursor = end + 1;
	}
	program->known = 1;
	return hs_program_check(program, err, err_cap);
}

int hs_pid_in_range(double kp, double ki, double kd) {
	return isfinite(kp) && isfinite(ki) && isfinite(kd) && kp >= 0 && ki >= 0 && kd >= 0 && kp <= 100 && ki <= 100 && kd <= 100;
}

int hs_parse_control_heat(const char *json, int *heat_known, int *heat_on, char *owner, size_t owner_cap) {
	char text[32];
	if (json == NULL) {
		return -1;
	}
	if (heat_known) {
		*heat_known = hs_json_has(json, "heat");
	}
	if (heat_on) {
		*heat_on = 0;
		if (hs_json_quoted(json, "heat", text, sizeof text)) {
			*heat_on = strcmp(text, "1") == 0 || strcmp(text, "true") == 0;
		}
	}
	if (owner && owner_cap) {
		if (!hs_json_quoted(json, "owner", owner, owner_cap)) {
			snprintf(owner, owner_cap, "未知");
		}
	}
	return 0;
}
