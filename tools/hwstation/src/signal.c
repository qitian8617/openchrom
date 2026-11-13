#include "hs.h"

#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static void map_reject(HsFaMap *map, HsMapStatus status) {
	map->status = status;
	map->fa = 0;
	map->pa = 0.0;
}

HsFaMap hs_map_linear_pa_to_fa(double pa) {
	HsFaMap map;
	long long rounded;
	double fa;
	if (!isfinite(pa)) {
		map_reject(&map, HS_MAP_NON_FINITE);
		return map;
	}
	fa = pa * 1000.0;
	if (!isfinite(fa) || fa > 2147483647.0 || fa < -2147483648.0) {
		map_reject(&map, HS_MAP_OVERFLOW);
		return map;
	}
	rounded = llround(fa);
	if (rounded > 2147483647LL || rounded < -2147483648LL) {
		map_reject(&map, HS_MAP_OVERFLOW);
		return map;
	}
	map.status = HS_MAP_OK;
	map.fa = (int32_t)rounded;
	map.pa = pa;
	return map;
}

HsFaMap hs_vlog_volts_to_fa(double vlog_volts) {
	double pa;
	if (!isfinite(vlog_volts)) {
		HsFaMap map;
		map_reject(&map, HS_MAP_NON_FINITE);
		return map;
	}
	pa = 100000.0 * pow(10.0, vlog_volts / 0.2);
	return hs_map_linear_pa_to_fa(pa);
}

HsFaMap hs_vlog_microvolts_to_fa(double vlog_uv) {
	if (!isfinite(vlog_uv)) {
		HsFaMap map;
		map_reject(&map, HS_MAP_NON_FINITE);
		return map;
	}
	return hs_vlog_volts_to_fa(vlog_uv / 1000000.0);
}

HsFaMap hs_map_sample_to_fa(HsUnit unit, double value) {
	HsFaMap map;
	if (unit == HS_UNIT_LINEAR_PA) {
		return hs_map_linear_pa_to_fa(value);
	}
	if (unit == HS_UNIT_VLOG_VOLT || unit == HS_UNIT_MV) {
		map_reject(&map, HS_MAP_VLOG_NOT_LINEAR);
		return map;
	}
	map_reject(&map, HS_MAP_UNKNOWN_UNIT);
	return map;
}

struct HsStream {
	HsSample items[HS_STREAM_CAP];
	int count;
	int aborted;
	char reason[160];
	int64_t last_t[2];
	int have_t[2];
	uint32_t last_count[2];
	int have_count[2];
	int64_t period_ms;
	int periods_seen;
	int dual;
	int ready[2];
	HsSample pending[2];
	int consumed;
};

HsStream *hs_stream_new(void) {
	HsStream *stream = calloc(1, sizeof *stream);
	if (stream) {
		stream->dual = 1;
	}
	return stream;
}

void hs_stream_free(HsStream *stream) {
	free(stream);
}

void hs_stream_set_channel_mode(HsStream *stream, int dual) {
	if (stream) {
		stream->dual = dual ? 1 : 0;
	}
}

static void abort_stream(HsStream *stream, const char *reason) {
	stream->aborted = 1;
	snprintf(stream->reason, sizeof stream->reason, "%s", reason);
}

int hs_stream_push(HsStream *stream, const HsSample *sample) {
	int ch;
	if (stream == NULL || sample == NULL) {
		return -1;
	}
	if (stream->aborted) {
		return -1;
	}
	if (sample->channel < 0 || sample->channel > 1 || !isfinite(sample->value)) {
		abort_stream(stream, "采样无效，已中止，未补点");
		return -1;
	}
	ch = sample->channel;
	if (stream->have_t[ch] && sample->t_ms < stream->last_t[ch]) {
		abort_stream(stream, "时间戳回退，已中止，未重排造点");
		return -1;
	}
	if (sample->has_count && stream->have_count[ch]) {
		uint32_t expect = stream->last_count[ch] + 1u;
		if (sample->count != expect) {
			abort_stream(stream, "计数缺口或乱序，已中止，未用 0 填实缺点");
			return -1;
		}
	}
	if (stream->have_t[ch]) {
		int64_t delta = sample->t_ms - stream->last_t[ch];
		if (delta <= 0) {
			abort_stream(stream, "采样间隔无效，已中止");
			return -1;
		}
		if (stream->periods_seen == 0) {
			stream->period_ms = delta;
			stream->periods_seen = 1;
		} else if (llabs(delta - stream->period_ms) > stream->period_ms / 5 + 1) {
			abort_stream(stream, "采样间隔突变，疑似丢点，已中止，未复制补点");
			return -1;
		}
	}
	if (stream->count >= HS_STREAM_CAP) {
		abort_stream(stream, "缓冲溢出，已中止投递，未覆盖旧点");
		return -1;
	}
	stream->items[stream->count++] = *sample;
	stream->last_t[ch] = sample->t_ms;
	stream->have_t[ch] = 1;
	if (sample->has_count) {
		stream->last_count[ch] = sample->count;
		stream->have_count[ch] = 1;
	}
	stream->pending[ch] = *sample;
	stream->ready[ch] = 1;
	return 0;
}

int hs_stream_aborted(const HsStream *stream) {
	return stream != NULL && stream->aborted;
}

const char *hs_stream_abort_reason(const HsStream *stream) {
	if (stream == NULL || !stream->aborted) {
		return "";
	}
	return stream->reason;
}

int hs_stream_count(const HsStream *stream) {
	return stream == NULL ? 0 : stream->count;
}

int hs_stream_period_ms(const HsStream *stream) {
	if (stream == NULL || stream->periods_seen == 0) {
		return 0;
	}
	return (int)stream->period_ms;
}

int hs_hw_interval_matches(int measured_ms, int confirmed_ms, int user_confirmed, char *why, size_t why_cap) {
	if (why && why_cap) {
		why[0] = '\0';
	}
	if (!user_confirmed || confirmed_ms <= 0) {
		if (why && why_cap) {
			snprintf(why, why_cap, "工作站采样间隔尚未由操作者核实。说明书默认 50 ms 不能当成已经对接，也不写 HWFrequence.txt");
		}
		return 0;
	}
	if (measured_ms <= 0) {
		if (why && why_cap) {
			snprintf(why, why_cap, "源周期未知，未按已核实的 %d ms 推送", confirmed_ms);
		}
		return 0;
	}
	if (measured_ms < confirmed_ms - 5 || measured_ms > confirmed_ms + 5) {
		if (why && why_cap) {
			snprintf(why, why_cap, "源周期 %d ms 与已核实的 %d ms 不一致。不写 HWFrequence.txt，不复制补点", measured_ms, confirmed_ms);
		}
		return 0;
	}
	return 1;
}

int hs_stream_next_hw_triple(HsStream *stream, int confirmed_ms, int user_confirmed, int32_t *data1, int32_t *data2, int32_t *data3, char *note, size_t note_cap) {
	char why[240];
	HsFaMap map;
	if (note && note_cap) {
		note[0] = '\0';
	}
	if (stream == NULL || stream->aborted) {
		return 0;
	}
	if (!hs_hw_interval_matches(hs_stream_period_ms(stream), confirmed_ms, user_confirmed, why, sizeof why)) {
		if (note && note_cap) {
			snprintf(note, note_cap, "%s", why);
		}
		return 0;
	}
	if (stream->dual) {
		if (!stream->ready[0] || !stream->ready[1]) {
			if ((stream->ready[0] ^ stream->ready[1]) && note && note_cap) {
				snprintf(note, note_cap, "双通道缺一路实测，未把缺通道填 0");
			}
			return 0;
		}
		if (stream->pending[0].t_ms != stream->pending[1].t_ms) {
			if (note && note_cap) {
				snprintf(note, note_cap, "双通道时间戳未配对，未填 0");
			}
			return 0;
		}
		map = hs_map_sample_to_fa(stream->pending[0].unit, stream->pending[0].value);
		if (map.status != HS_MAP_OK) {
			abort_stream(stream, "通道量纲不能映射为 fA，已中止");
			return 0;
		}
		*data1 = map.fa;
		map = hs_map_sample_to_fa(stream->pending[1].unit, stream->pending[1].value);
		if (map.status != HS_MAP_OK) {
			abort_stream(stream, "通道量纲不能映射为 fA，已中止");
			return 0;
		}
		*data2 = map.fa;
		*data3 = 0;
		stream->ready[0] = 0;
		stream->ready[1] = 0;
		if (note && note_cap) {
			snprintf(note, note_cap, "data3=0 双通道整数点");
		}
		return 1;
	}
	if (!stream->ready[0]) {
		return 0;
	}
	map = hs_map_sample_to_fa(stream->pending[0].unit, stream->pending[0].value);
	if (map.status != HS_MAP_OK) {
		abort_stream(stream, "通道量纲不能映射为 fA，已中止");
		return 0;
	}
	*data1 = map.fa;
	*data2 = 0;
	*data3 = 3;
	stream->ready[0] = 0;
	if (note && note_cap) {
		snprintf(note, note_cap, "data3=3 单通道；data2=0 是协议占位，不是实测零点");
	}
	return 1;
}

int hs_pack_triple(const HsTriple *triple, uint8_t out[12]) {
	int i;
	int32_t values[3];
	if (triple == NULL || out == NULL) {
		return -1;
	}
	values[0] = triple->data1;
	values[1] = triple->data2;
	values[2] = triple->data3;
	for (i = 0; i < 3; i++) {
		uint32_t v = (uint32_t)values[i];
		out[i * 4] = (uint8_t)(v & 0xFF);
		out[i * 4 + 1] = (uint8_t)((v >> 8) & 0xFF);
		out[i * 4 + 2] = (uint8_t)((v >> 16) & 0xFF);
		out[i * 4 + 3] = (uint8_t)((v >> 24) & 0xFF);
	}
	return 12;
}

int hs_unpack_triple(const uint8_t in[12], HsTriple *triple) {
	uint32_t v[3];
	int i;
	if (in == NULL || triple == NULL) {
		return -1;
	}
	for (i = 0; i < 3; i++) {
		v[i] = (uint32_t)in[i * 4] | ((uint32_t)in[i * 4 + 1] << 8) | ((uint32_t)in[i * 4 + 2] << 16) | ((uint32_t)in[i * 4 + 3] << 24);
	}
	triple->data1 = (int32_t)v[0];
	triple->data2 = (int32_t)v[1];
	triple->data3 = (int32_t)v[2];
	return 0;
}

void hs_copydata_image(const HsTriple *triple, uint32_t *dw_data, uint32_t *cb_data, uint8_t payload[12]) {
	if (dw_data) {
		*dw_data = HS_COPYDATA_MAGIC;
	}
	if (cb_data) {
		*cb_data = 12;
	}
	if (triple && payload) {
		hs_pack_triple(triple, payload);
	}
}

const char *hs_sdk_return_meaning(int dll_return) {
	if (dll_return == 1) {
		return "返回 1 只表示找到窗口并发出 WM_COPYDATA，不表示谱图采集成功";
	}
	if (dll_return == 0) {
		return "返回 0 表示未找到目标窗口，消息未发出";
	}
	return "返回值不是源码中的 0/1，结果未知";
}
