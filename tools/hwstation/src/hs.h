/*******************************************************************************
 * Copyright (c) 2026 OpenChrom contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Standalone HW-2000 chromatogram bridge + GC workstation remote panel.
 * Behavior is reimplemented from the temperature-control plugin in this
 * repository and from the HW-2000 SDK notes supplied with the task.
 * It is not a claim that HW-2000GMP has been jointly tested.
 *******************************************************************************/
#ifndef HWSTATION_HS_H
#define HWSTATION_HS_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

#define HS_CTRL_WINDOW_CLASS "ControlPanelClass"
#define HS_CTRL_EXE_NAME "CtrlPanel.exe"
#define HS_BRIDGE_EXE_NAME "hwbridge.exe"
#define HS_FAKE_TEST_CLASS "HwstationFakeHw"
#define HS_COPYDATA_MAGIC 0xCCCCu
#define HS_PRESSURE_MIN_MPA 0.1
#define HS_PRESSURE_FRESH_MS 3000
#define HS_STATUS_FRESH_MS 3000
#define HS_HW_DEFAULT_INTERVAL_MS 50
#define HS_DEVICE_ACQ_REQUEST_MS 100
#define HS_OVEN_STAGES 8
#define HS_JSON_MAX 1536
#define HS_TEMP_CEILING_C 450.0
#define HS_AUX_STABLE_BAND_C 3.0
#define HS_AUX_STABLE_POLLS 10
#define HS_STREAM_CAP 256
#define HS_LOG_CAP_BYTES (256 * 1024)
#define HS_LOG_FILES 3
#define HS_AUTH_MIN_ITERS 210000
#define HS_AUTH_MAX_ITERS 2000000
#define HS_AUTH_ITERS 600000
#define HS_AUTH_MIN_PASSWORD 10
#define HS_FA_FULL_SCALE_PA 2147.483647

typedef struct HsRect {
	int x;
	int y;
	int w;
	int h;
} HsRect;

int hs_layout_buttons(HsRect *out, int count, int origin_x, int origin_y, int width, int min_btn_w, int btn_h, int gap);
int hs_layout_columns(int *widths, const int *min_widths, int count, int total);

typedef enum HsCmdKind {
	HS_CMD_READ = 0,
	HS_CMD_SAFETY,
	HS_CMD_OPERATOR,
	HS_CMD_ENGINEERING,
	HS_CMD_UNSUPPORTED
} HsCmdKind;

HsCmdKind hs_classify_command(const char *line);
int hs_command_allowed_when_locked(const char *line);
int hs_payload_ok(const char *payload);
int hs_binary_opcode_ok(unsigned command);
const char *hs_command_verb(const char *line);

typedef enum HsHeatGate {
	HS_HEAT_ALLOW = 0,
	HS_HEAT_BLOCK_UNKNOWN,
	HS_HEAT_BLOCK_STALE,
	HS_HEAT_BLOCK_NON_FINITE,
	HS_HEAT_BLOCK_BELOW
} HsHeatGate;

HsHeatGate hs_heat_start_gate(int h2_known, double h2_mpa, int air_known, double air_mpa, int64_t sample_ms, int64_t now_ms);
const char *hs_heat_gate_text(HsHeatGate gate);

typedef enum HsUnit {
	HS_UNIT_UNKNOWN = 0,
	HS_UNIT_LINEAR_PA,
	HS_UNIT_VLOG_VOLT,
	HS_UNIT_MV
} HsUnit;

typedef enum HsMapStatus {
	HS_MAP_OK = 0,
	HS_MAP_UNKNOWN_UNIT,
	HS_MAP_VLOG_NOT_LINEAR,
	HS_MAP_NON_FINITE,
	HS_MAP_OVERFLOW
} HsMapStatus;

typedef struct HsFaMap {
	HsMapStatus status;
	int32_t fa;
	double pa;
} HsFaMap;

HsFaMap hs_map_linear_pa_to_fa(double pa);
HsFaMap hs_map_sample_to_fa(HsUnit unit, double value);
HsFaMap hs_vlog_volts_to_fa(double vlog_volts);
HsFaMap hs_vlog_microvolts_to_fa(double vlog_uv);

typedef struct HsSample {
	int64_t t_ms;
	uint32_t count;
	int has_count;
	double value;
	HsUnit unit;
	int channel;
} HsSample;

typedef struct HsStream HsStream;

HsStream *hs_stream_new(void);
void hs_stream_free(HsStream *stream);
void hs_stream_set_channel_mode(HsStream *stream, int dual);
int hs_stream_push(HsStream *stream, const HsSample *sample);
int hs_stream_aborted(const HsStream *stream);
const char *hs_stream_abort_reason(const HsStream *stream);
int hs_stream_count(const HsStream *stream);
int hs_stream_period_ms(const HsStream *stream);
int hs_hw_interval_matches(int measured_ms, int confirmed_ms, int user_confirmed, char *why, size_t why_cap);
int hs_stream_next_hw_triple(HsStream *stream, int confirmed_ms, int user_confirmed, int32_t *data1, int32_t *data2, int32_t *data3, char *note, size_t note_cap);

typedef enum HsPressureKind {
	HS_PRESSURE_ABSENT = 0,
	HS_PRESSURE_NAMED_MPA,
	HS_PRESSURE_ADC_UNIT_UNCONFIRMED
} HsPressureKind;

int hs_parse_gas_pressure(const char *json, double *h2, double *air, HsPressureKind *h2_kind, HsPressureKind *air_kind);

typedef struct HsProgram {
	int known;
	double max_c;
	double eq_min;
	double stable_sec;
	int loop;
	double rate[HS_OVEN_STAGES];
	double temperature[HS_OVEN_STAGES];
	double hold[HS_OVEN_STAGES];
} HsProgram;

int hs_program_check(const HsProgram *program, char *err, size_t err_cap);
long hs_program_duration_ms(const HsProgram *program);
int hs_program_from_json(const char *json, HsProgram *program, char *err, size_t err_cap);

typedef struct HsPidRow {
	int known;
	double kp;
	double ki;
	double kd;
} HsPidRow;

int hs_pid_in_range(double kp, double ki, double kd);
int hs_parse_control_heat(const char *json, int *heat_known, int *heat_on, char *owner, size_t owner_cap);

typedef struct HsTriple {
	int32_t data1;
	int32_t data2;
	int32_t data3;
} HsTriple;

int hs_pack_triple(const HsTriple *triple, uint8_t out[12]);
int hs_unpack_triple(const uint8_t in[12], HsTriple *triple);
void hs_copydata_image(const HsTriple *triple, uint32_t *dw_data, uint32_t *cb_data, uint8_t payload[12]);
const char *hs_sdk_return_meaning(int dll_return);

typedef struct HsCrcFrame HsCrcFrame;
int hs_crc16(const uint8_t *data, int offset, int length);
int hs_frame_encode(unsigned command, unsigned sequence, const uint8_t *payload, int payload_len, uint8_t *out, int out_cap);
int hs_frame_decode(const uint8_t *frame, int frame_len, unsigned *command, unsigned *sequence, uint8_t *payload, int payload_cap, int *payload_len);

int hs_acq_encode_start(int interval_ms, uint8_t *out, int out_cap);
int hs_acq_encode_batch(int batch_sequence, const int *times_ms, const float *signals, int count, uint8_t *out, int out_cap);
int hs_acq_decode_batch(const uint8_t *payload, int payload_len, int *batch_sequence, int *times_ms, float *signals, int cap, int *count);

int hs_json_quoted(const char *json, const char *key, char *out, size_t cap);
int hs_json_has(const char *json, const char *key);
double hs_json_number(const char *json, const char *key, int *ok);
int hs_format_number(double value, char *out, size_t cap);
int hs_oven_program_json(double max_c, double eq_min, double stable_sec, int loop, const double *rate, const double *temp, const double *hold, char *out, size_t cap);
int hs_pid_json(double kp, double ki, double kd, char *out, size_t cap);
int hs_setpoint_ok(double celsius);

typedef enum HsFlame {
	HS_FLAME_UNKNOWN = 0,
	HS_FLAME_OUT,
	HS_FLAME_ON,
	HS_FLAME_IGNITING,
	HS_FLAME_FAIL,
	HS_FLAME_OFFLINE,
	HS_FLAME_STALE
} HsFlame;

HsFlame hs_flame_from_status(int known, int online, int busy, int flame, const char *state, int64_t sample_ms, int64_t now_ms);
const char *hs_flame_text(HsFlame flame);

typedef struct HsAuth HsAuth;

HsAuth *hs_auth_open(const char *path);
void hs_auth_set_idle_ms(HsAuth *auth, int64_t idle_ms);
void hs_auth_close(HsAuth *auth);
int hs_auth_missing(const HsAuth *auth);
int hs_auth_corrupt(const HsAuth *auth);
int hs_auth_allows(HsAuth *auth, int64_t now_ms);
void hs_auth_lock(HsAuth *auth, int64_t now_ms);
void hs_auth_touch(HsAuth *auth, int64_t now_ms);
int hs_auth_enroll(HsAuth *auth, const char *password, const char *confirm, int64_t now_ms, int iterations);
int hs_auth_verify(HsAuth *auth, const char *password, int64_t now_ms);
const char *hs_auth_status(const HsAuth *auth, int64_t now_ms);

int hs_pbkdf2_sha256(const uint8_t *password, size_t password_len, const uint8_t *salt, size_t salt_len, int iterations, uint8_t out[32]);

typedef struct HsSession HsSession;
typedef struct HsIo {
	int (*write)(void *ctx, const void *buf, int len);
	int (*read)(void *ctx, void *buf, int cap, int timeout_ms);
	void *ctx;
} HsIo;

typedef struct HsSnapshot {
	char mode[32];
	char link[96];
	char heat[96];
	char stop_confirm[120];
	char device_acq[80];
	char hw[180];
	char alarm[240];
	char banner[280];
	char flame[64];
	char fid_pa[80];
	char h2[80];
	char air[80];
	char inlet_set[32];
	char inlet_act[32];
	char inlet_state[40];
	char det_set[32];
	char det_act[32];
	char det_state[40];
	char oven_set[32];
	char oven_act[32];
	char oven_state[48];
	char engineer[80];
	char pace[160];
	char unit_note[200];
	char owner[32];
	int heat_running;
	int connected;
	int alarm_on;
	int net_writes;
	int sim;
} HsSnapshot;

HsSession *hs_session_create(const char *dir);
HsSession *hs_session_create_with_io(const char *dir, HsIo *io);
void hs_session_free(HsSession *session);
int hs_instance_try_lock(const char *dir);
void hs_instance_unlock(const char *dir);

int hs_session_set_sim(HsSession *session);
int hs_session_set_offline(HsSession *session);
int hs_session_use_scripted_live(HsSession *session);
int hs_session_connect_live(HsSession *session, const char *host, int port, char *err, size_t err_cap);
void hs_session_disconnect(HsSession *session);
int hs_session_poll(HsSession *session);
void hs_session_tick(HsSession *session, int64_t now_ms);
void hs_session_snapshot(HsSession *session, int64_t now_ms, HsSnapshot *out);
int hs_session_net_writes(const HsSession *session);

int hs_session_start_heat(HsSession *session, int inlet, int detector, int oven, double inlet_c, double detector_c, int64_t now_ms, char *err, size_t err_cap);
int hs_session_stop_heat(HsSession *session, int64_t now_ms, char *err, size_t err_cap);
int hs_session_estop(HsSession *session, int64_t now_ms, char *err, size_t err_cap);
int hs_session_ignite(HsSession *session, char *err, size_t err_cap);
int hs_session_valves(HsSession *session, int open, char *err, size_t err_cap);
int hs_session_fid_auto(HsSession *session, int enable, char *err, size_t err_cap);
int hs_session_write_program(HsSession *session, const char *spec, char *err, size_t err_cap);
int hs_session_set_program(HsSession *session, const HsProgram *program, char *err, size_t err_cap);
int hs_session_read_program(HsSession *session, char *err, size_t err_cap);
int hs_session_write_program_now(HsSession *session, char *err, size_t err_cap);
void hs_session_get_program(const HsSession *session, HsProgram *program);
int hs_session_set_pid(HsSession *session, int which, double kp, double ki, double kd, char *err, size_t err_cap);
int hs_session_read_pids(HsSession *session, char *err, size_t err_cap);
void hs_session_pid_confirm_text(const HsSession *session, char *out, size_t cap);
int hs_session_write_pids_confirmed(HsSession *session, const char *confirm, int64_t now_ms, char *err, size_t err_cap);
void hs_session_get_pid(const HsSession *session, int which, HsPidRow *row);
int hs_session_observe_aux(HsSession *session, int64_t now_ms, int inlet_fresh, double inlet_act, int det_fresh, double det_act, int oven_fault);
int hs_session_set_hw_interval(HsSession *session, int interval_ms, int confirmed, char *err, size_t err_cap);
int hs_session_probe(HsSession *session, const char *host, int port, char *err, size_t err_cap);
int hs_session_ingest_pressure_json(HsSession *session, const char *json, int64_t now_ms);
int hs_session_write_setpoints(HsSession *session, double inlet_c, double detector_c, char *err, size_t err_cap);
int hs_session_device_apply_blocked(char *err, size_t err_cap);
void hs_session_event_text(const HsSession *session, char *out, size_t cap);
void hs_session_stopwatch_text(HsSession *session, int64_t now_ms, char *out, size_t cap);
void hs_session_reset_stopwatch(HsSession *session);
int hs_session_trace_count(const HsSession *session);
int hs_session_trace_at(const HsSession *session, int index, double *inlet, double *detector, double *oven);
int hs_session_start_device_acq(HsSession *session, char *err, size_t err_cap);
int hs_session_stop_device_acq(HsSession *session, char *err, size_t err_cap);

void hs_session_set_sim_flame(HsSession *session, HsFlame flame);
void hs_session_set_sim_pressure(HsSession *session, double h2, double air, int known);
void hs_session_set_sim_period_ms(HsSession *session, int period_ms);
void hs_session_arm_linear_pa(HsSession *session, int armed);
int hs_session_linear_pa_armed(const HsSession *session);

int hs_session_hw_start(HsSession *session, HsTriple *sent_or_null, char *err, size_t err_cap);
int hs_session_push_sample(HsSession *session, const HsSample *sample);
int hs_session_poll_hw_sample(HsSession *session, HsTriple *out, char *note, size_t note_cap);
int hs_session_hw_stop(HsSession *session, char *err, size_t err_cap);
int hs_session_hw_zero(HsSession *session, char *err, size_t err_cap);
int hs_session_note_hwsys(HsSession *session, const char *path, int refreshed, int timed_out);
int hs_session_write_report(HsSession *session, const char *path, int64_t now_ms);

int hs_session_enroll(HsSession *session, const char *password, const char *confirm, int64_t now_ms, char *err, size_t err_cap);
int hs_session_verify(HsSession *session, const char *password, int64_t now_ms, char *err, size_t err_cap);
void hs_session_lock_engineer(HsSession *session, int64_t now_ms);
int hs_session_save_column_note(HsSession *session, const char *text, int64_t now_ms, char *err, size_t err_cap);

int hs_session_log_bytes(const HsSession *session);
void hs_session_trace(HsSession *session, const char *line);
const char *hs_session_written(const HsSession *session);
const char *hs_session_recovery_banner(HsSession *session);

int hs_host_port_ok(const char *host, int port, char *err, size_t err_cap);
void hs_hw_class_name(int index, char *out, size_t cap);
int hs_hw_export_blocked(int index);
int hs_utf8_to_wide(const char *utf8, wchar_t *wide, size_t wide_cap);

#ifdef __cplusplus
}
#endif

#endif
