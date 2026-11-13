#include "hs.h"

#include <windows.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static HsSession *g_session;
static HFONT g_font;
static int g_page;
static HWND g_pages[5][48];
static int g_page_count[5];
static HWND g_link, g_heat, g_stop, g_acq, g_alarm, g_banner;
static HWND g_zones[3][4];
static HWND g_host, g_port, g_inlet, g_det;
static HWND g_prog[8][3];
static HWND g_prog_max, g_prog_eq, g_prog_stable, g_prog_loop, g_prog_time;
static HWND g_pid[3][3];
static HWND g_pid_state;
static HWND g_pass, g_pass2;
static HWND g_dll, g_hw_ms, g_hw_ok;
static HWND g_events, g_clock, g_trend;
static HWND g_note, g_cal_in, g_cal_det;
static HWND g_nav[5];
static HANDLE g_bridge_in, g_bridge_out, g_bridge_process;
static int g_smoke;

static void set_utf8(HWND hwnd, const char *text) {
	wchar_t wide[500];
	if (!hwnd) {
		return;
	}
	if (hs_utf8_to_wide(text ? text : "", wide, 500) < 0) {
		SetWindowTextW(hwnd, L"");
		return;
	}
	SetWindowTextW(hwnd, wide);
}

static void get_utf8(HWND hwnd, char *out, size_t cap) {
	wchar_t wide[240];
	size_t o = 0;
	int n;
	GetWindowTextW(hwnd, wide, 240);
	for (n = 0; wide[n] && o + 4 < cap; n++) {
		unsigned c = (unsigned)wide[n];
		if (c < 0x80) {
			out[o++] = (char)c;
		} else if (c < 0x800) {
			out[o++] = (char)(0xC0 | (c >> 6));
			out[o++] = (char)(0x80 | (c & 0x3F));
		} else {
			out[o++] = (char)(0xE0 | (c >> 12));
			out[o++] = (char)(0x80 | ((c >> 6) & 0x3F));
			out[o++] = (char)(0x80 | (c & 0x3F));
		}
	}
	out[o] = '\0';
}

static double edit_number(HWND hwnd, int *ok) {
	char text[64];
	char *end = NULL;
	double value;
	get_utf8(hwnd, text, sizeof text);
	if (text[0] == '\0') {
		if (ok) {
			*ok = 0;
		}
		return 0;
	}
	value = strtod(text, &end);
	if (ok) {
		*ok = end != text && *end == '\0';
	}
	return value;
}

static void show_result(const char *text) {
	wchar_t wide[500];
	hs_utf8_to_wide(text ? text : "", wide, 500);
	MessageBoxW(NULL, wide, L"反控", MB_OK);
}

static HWND make_check(HWND parent, int page, const wchar_t *text, int id) {
	HWND hwnd = CreateWindowExW(0, L"BUTTON", text, WS_CHILD | WS_TABSTOP | BS_AUTOCHECKBOX, 0, 0, 120, 24, parent, (HMENU)(INT_PTR)id, GetModuleHandleW(NULL), NULL);
	SendMessageW(hwnd, WM_SETFONT, (WPARAM)g_font, TRUE);
	if (page >= 0) {
		g_pages[page][g_page_count[page]++] = hwnd;
	}
	return hwnd;
}

static HWND make_button(HWND parent, int page, const wchar_t *text, int id) {
	HWND hwnd = CreateWindowExW(0, L"BUTTON", text, WS_CHILD | WS_TABSTOP, 0, 0, 80, 28, parent, (HMENU)(INT_PTR)id, GetModuleHandleW(NULL), NULL);
	SendMessageW(hwnd, WM_SETFONT, (WPARAM)g_font, TRUE);
	g_pages[page][g_page_count[page]++] = hwnd;
	return hwnd;
}

static HWND make_edit(HWND parent, int page, int id) {
	HWND hwnd = CreateWindowExW(WS_EX_CLIENTEDGE, L"EDIT", L"", WS_CHILD | ES_AUTOHSCROLL, 0, 0, 70, 22, parent, (HMENU)(INT_PTR)id, GetModuleHandleW(NULL), NULL);
	SendMessageW(hwnd, WM_SETFONT, (WPARAM)g_font, TRUE);
	g_pages[page][g_page_count[page]++] = hwnd;
	return hwnd;
}

static HWND make_static(HWND parent, int page, const wchar_t *text) {
	HWND hwnd = CreateWindowExW(0, L"STATIC", text, WS_CHILD, 0, 0, 80, 18, parent, NULL, GetModuleHandleW(NULL), NULL);
	SendMessageW(hwnd, WM_SETFONT, (WPARAM)g_font, TRUE);
	if (page >= 0) {
		g_pages[page][g_page_count[page]++] = hwnd;
	}
	return hwnd;
}

static void show_page(int page) {
	int p;
	int i;
	g_page = page;
	for (p = 0; p < 5; p++) {
		for (i = 0; i < g_page_count[p]; i++) {
			ShowWindow(g_pages[p][i], p == page ? SW_SHOW : SW_HIDE);
		}
	}
}

static int text_px(HWND hwnd) {
	wchar_t text[96];
	SIZE size;
	HDC dc;
	HFONT old;
	int len;
	GetWindowTextW(hwnd, text, 96);
	len = (int)wcslen(text);
	if (len <= 0) {
		return 0;
	}
	dc = GetDC(hwnd);
	if (dc == NULL) {
		return len * 16;
	}
	{
		HGDIOBJ font = g_font != NULL ? (HGDIOBJ)g_font : GetStockObject(DEFAULT_GUI_FONT);
		old = (HFONT)SelectObject(dc, font);
	}
	GetTextExtentPoint32W(dc, text, len, &size);
	SelectObject(dc, old);
	ReleaseDC(hwnd, dc);
	return (int)size.cx;
}

static int control_span(HWND hwnd) {
	wchar_t klass[32];
	int text = text_px(hwnd);
	LONG style;
	GetClassNameW(hwnd, klass, 32);
	if (wcscmp(klass, L"Edit") == 0) {
		return 96;
	}
	style = GetWindowLongW(hwnd, GWL_STYLE);
	if (wcscmp(klass, L"Button") == 0 && (style & 0x0F) == BS_AUTOCHECKBOX) {
		return text + 28;
	}
	if (wcscmp(klass, L"Button") == 0) {
		return text + 22;
	}
	return text + 12;
}

static void place_flow(int page, int left, int top, int width) {
	int x = left;
	int y = top;
	int i;
	int gap = 6;
	int row_h = 28;
	if (width < 40) {
		width = 40;
	}
	for (i = 0; i < g_page_count[page]; i++) {
		HWND item = g_pages[page][i];
		int span;
		if (item == NULL || item == g_events || item == g_trend) {
			continue;
		}
		span = control_span(item);
		if (span < 40) {
			span = 40;
		}
		if (x > left && x + span > left + width) {
			x = left;
			y += row_h + gap;
		}
		MoveWindow(item, x, y, span, row_h, TRUE);
		x += span + gap;
	}
}

static int bridge_write_line(const char *cmd, char *reply, int cap);

static int bridge_read_line(char *out, int cap) {
	int n = 0;
	while (n + 1 < cap) {
		DWORD got = 0;
		char ch = 0;
		if (!ReadFile(g_bridge_out, &ch, 1, &got, NULL) || got != 1) {
			return -1;
		}
		if (ch == '\n') {
			break;
		}
		if (ch != '\r') {
			out[n++] = ch;
		}
	}
	out[n] = '\0';
	return 0;
}

static int bridge_start(void) {
	wchar_t exe[MAX_PATH];
	if (g_smoke) {
		return -1;
	}
	wchar_t bridge[MAX_PATH];
	SECURITY_ATTRIBUTES sa;
	STARTUPINFOW si;
	PROCESS_INFORMATION pi;
	HANDLE read_out, write_out, read_in, write_in;
	char ready[32];
	if (g_bridge_process) {
		return 0;
	}
	GetModuleFileNameW(NULL, exe, MAX_PATH);
	{
		wchar_t *slash = wcsrchr(exe, L'\\');
		if (slash) {
			*slash = 0;
		}
	}
	_snwprintf(bridge, MAX_PATH, L"%s\\hwbridge.exe", exe);
	memset(&sa, 0, sizeof sa);
	sa.nLength = (DWORD)sizeof sa;
	sa.bInheritHandle = TRUE;
	if (!CreatePipe(&read_out, &write_out, &sa, 0) || !CreatePipe(&read_in, &write_in, &sa, 0)) {
		return -1;
	}
	SetHandleInformation(read_in, HANDLE_FLAG_INHERIT, 0);
	SetHandleInformation(write_out, HANDLE_FLAG_INHERIT, 0);
	memset(&si, 0, sizeof si);
	si.cb = (DWORD)sizeof si;
	si.dwFlags = STARTF_USESTDHANDLES;
	si.hStdInput = read_in;
	si.hStdOutput = write_out;
	si.hStdError = write_out;
	memset(&pi, 0, sizeof pi);
	if (!CreateProcessW(bridge, NULL, NULL, NULL, TRUE, CREATE_NO_WINDOW, NULL, NULL, &si, &pi)) {
		return -1;
	}
	CloseHandle(read_in);
	CloseHandle(write_out);
	CloseHandle(pi.hThread);
	g_bridge_in = write_in;
	g_bridge_out = read_out;
	g_bridge_process = pi.hProcess;
	return bridge_read_line(ready, sizeof ready) == 0 && strcmp(ready, "READY") == 0 ? 0 : -1;
}

static void fill_program_edits(void) {
	HsProgram program;
	char text[32];
	int stage;
	int col;
	hs_session_get_program(g_session, &program);
	if (!program.known) {
		set_utf8(g_prog_time, "温程未读取");
		return;
	}
	snprintf(text, sizeof text, "%.4g", program.max_c);
	set_utf8(g_prog_max, text);
	snprintf(text, sizeof text, "%.4g", program.eq_min);
	set_utf8(g_prog_eq, text);
	snprintf(text, sizeof text, "%.4g", program.stable_sec);
	set_utf8(g_prog_stable, text);
	SendMessageW(g_prog_loop, BM_SETCHECK, program.loop ? BST_CHECKED : BST_UNCHECKED, 0);
	for (stage = 0; stage < 8; stage++) {
		double values[3] = {program.rate[stage], program.temperature[stage], program.hold[stage]};
		for (col = 0; col < 3; col++) {
			snprintf(text, sizeof text, "%.4g", values[col]);
			set_utf8(g_prog[stage][col], text);
		}
	}
	snprintf(text, sizeof text, "预计 %.1f min", hs_program_duration_ms(&program) / 60000.0);
	set_utf8(g_prog_time, text);
}

static int read_program_edits(HsProgram *program, char *err, size_t cap) {
	int stage;
	int ok = 0;
	memset(program, 0, sizeof *program);
	program->max_c = edit_number(g_prog_max, &ok);
	if (!ok) {
		snprintf(err, cap, "最高温度为空");
		return -1;
	}
	program->eq_min = edit_number(g_prog_eq, &ok);
	if (!ok) {
		snprintf(err, cap, "平衡时间为空");
		return -1;
	}
	program->stable_sec = edit_number(g_prog_stable, &ok);
	if (!ok) {
		snprintf(err, cap, "稳定时间为空");
		return -1;
	}
	program->loop = SendMessageW(g_prog_loop, BM_GETCHECK, 0, 0) == BST_CHECKED;
	for (stage = 0; stage < 8; stage++) {
		program->rate[stage] = edit_number(g_prog[stage][0], &ok);
		if (!ok) {
			snprintf(err, cap, "第 %d 阶速率为空", stage + 1);
			return -1;
		}
		program->temperature[stage] = edit_number(g_prog[stage][1], &ok);
		if (!ok) {
			snprintf(err, cap, "第 %d 阶温度为空", stage + 1);
			return -1;
		}
		program->hold[stage] = edit_number(g_prog[stage][2], &ok);
		if (!ok) {
			snprintf(err, cap, "第 %d 阶保持为空", stage + 1);
			return -1;
		}
	}
	return hs_program_check(program, err, cap);
}

static void fill_pid_edits(void) {
	int row;
	int known = 1;
	for (row = 0; row < 3; row++) {
		HsPidRow pid;
		char text[32];
		hs_session_get_pid(g_session, row, &pid);
		if (!pid.known) {
			known = 0;
			set_utf8(g_pid[row][0], "");
			set_utf8(g_pid[row][1], "");
			set_utf8(g_pid[row][2], "");
			continue;
		}
		snprintf(text, sizeof text, "%.4g", pid.kp);
		set_utf8(g_pid[row][0], text);
		snprintf(text, sizeof text, "%.4g", pid.ki);
		set_utf8(g_pid[row][1], text);
		snprintf(text, sizeof text, "%.4g", pid.kd);
		set_utf8(g_pid[row][2], text);
	}
	set_utf8(g_pid_state, known ? "三路已有数值，写入前仍要确认" : "有路未读取/未填写，不能写入");
}

static void refresh(HWND hwnd) {
	HsSnapshot snap;
	int64_t now = (int64_t)GetTickCount64();
	char line[300];
	char clock_text[32];
	(void)hwnd;
	if (g_session == NULL) {
		return;
	}
	hs_session_tick(g_session, now);
	hs_session_snapshot(g_session, now, &snap);
	set_utf8(g_link, snap.link);
	set_utf8(g_heat, snap.heat);
	set_utf8(g_stop, snap.stop_confirm);
	snprintf(line, sizeof line, "%s | %s", snap.device_acq, snap.hw);
	set_utf8(g_acq, line);
	snprintf(line, sizeof line, "%s | H2 %s | 空气 %s | 火焰 %s | %s", snap.banner[0] ? snap.banner : snap.mode, snap.h2, snap.air, snap.flame, snap.engineer);
	set_utf8(g_banner, line);
	set_utf8(g_alarm, snap.alarm_on ? snap.alarm : snap.pace);
	set_utf8(g_zones[0][1], snap.inlet_set);
	set_utf8(g_zones[0][2], snap.inlet_act);
	set_utf8(g_zones[0][3], snap.inlet_state);
	set_utf8(g_zones[1][1], snap.det_set);
	set_utf8(g_zones[1][2], snap.det_act);
	set_utf8(g_zones[1][3], snap.det_state);
	set_utf8(g_zones[2][1], snap.oven_set);
	set_utf8(g_zones[2][2], snap.oven_act);
	set_utf8(g_zones[2][3], snap.oven_state);
	hs_session_stopwatch_text(g_session, now, clock_text, sizeof clock_text);
	set_utf8(g_clock, clock_text);
	{
		char events[1200];
		hs_session_event_text(g_session, events, sizeof events);
		set_utf8(g_events, events);
	}
	InvalidateRect(g_trend, NULL, TRUE);
}

static LRESULT CALLBACK trend_proc(HWND hwnd, UINT msg, WPARAM wparam, LPARAM lparam) {
	if (msg == WM_PAINT) {
		PAINTSTRUCT ps;
		HDC dc = BeginPaint(hwnd, &ps);
		RECT rect;
		int count;
		int i;
		GetClientRect(hwnd, &rect);
		FillRect(dc, &rect, (HBRUSH)(COLOR_WINDOW + 1));
		count = hs_session_trace_count(g_session);
		if (count > 1 && rect.right > 4 && rect.bottom > 4) {
			HPEN pens[3];
			pens[0] = CreatePen(PS_SOLID, 1, RGB(180, 40, 40));
			pens[1] = CreatePen(PS_SOLID, 1, RGB(30, 90, 160));
			pens[2] = CreatePen(PS_SOLID, 1, RGB(20, 120, 60));
			for (i = 0; i < 3; i++) {
				int index;
				SelectObject(dc, pens[i]);
				for (index = 0; index < count; index++) {
					double inlet, detector, oven, value;
					hs_session_trace_at(g_session, index, &inlet, &detector, &oven);
					value = i == 0 ? inlet : i == 1 ? detector : oven;
					if (value < 0) {
						continue;
					}
					{
						int x = rect.left + index * (rect.right - rect.left) / count;
						int y = rect.bottom - (int)(value * (rect.bottom - rect.top) / 450.0);
						if (index == 0) {
							MoveToEx(dc, x, y, NULL);
						} else {
							LineTo(dc, x, y);
						}
					}
				}
			}
			for (i = 0; i < 3; i++) {
				DeleteObject(pens[i]);
			}
		}
		EndPaint(hwnd, &ps);
		return 0;
	}
	return DefWindowProcW(hwnd, msg, wparam, lparam);
}

static void layout(HWND hwnd);

static void smoke_log_path(char *path, size_t cap) {
	DWORD n;
	char *slash;
	if (path == NULL || cap == 0) {
		return;
	}
	path[0] = '\0';
	n = GetModuleFileNameA(NULL, path, (DWORD)cap);
	if (n == 0 || n >= cap) {
		path[0] = '\0';
		return;
	}
	slash = strrchr(path, '\\');
	if (slash == NULL) {
		slash = strrchr(path, '/');
	}
	if (slash == NULL || (size_t)(slash - path) + 18 >= cap) {
		path[0] = '\0';
		return;
	}
	memcpy(slash + 1, "offline-smoke.txt", 18);
}

static void smoke_line(const char *text) {
	HANDLE out;
	DWORD written = 0;
	char path[MAX_PATH];
	FILE *fp;
	AttachConsole(ATTACH_PARENT_PROCESS);
	out = GetStdHandle(STD_OUTPUT_HANDLE);
	if (out != NULL && out != INVALID_HANDLE_VALUE && text != NULL) {
		WriteFile(out, text, (DWORD)strlen(text), &written, NULL);
	}
	smoke_log_path(path, sizeof path);
	fp = path[0] ? fopen(path, "ab") : NULL;
	if (fp != NULL) {
		fputs(text ? text : "", fp);
		fclose(fp);
	}
}

static void smoke_reset_log(void) {
	char path[MAX_PATH];
	FILE *fp;
	smoke_log_path(path, sizeof path);
	fp = path[0] ? fopen(path, "wb") : NULL;
	if (fp != NULL) {
		fclose(fp);
	}
}

static void wide_utf8(const wchar_t *wide, char *out, int cap) {
	if (out == NULL || cap <= 0) {
		return;
	}
	if (WideCharToMultiByte(CP_UTF8, 0, wide ? wide : L"", -1, out, cap, NULL, NULL) <= 0) {
		out[0] = '\0';
	}
}

static HWND find_child_text(HWND parent, const wchar_t *text) {
	HWND child = GetWindow(parent, GW_CHILD);
	wchar_t buf[80];
	while (child) {
		GetWindowTextW(child, buf, 80);
		if (wcscmp(buf, text) == 0) {
			return child;
		}
		child = GetWindow(child, GW_HWNDNEXT);
	}
	return NULL;
}

static int smoke_check(HWND hwnd) {
	static const struct {
		int page;
		const wchar_t *tab;
		const wchar_t *text;
	} need[] = {
		{0, L"反控", L"连接仪器"},
		{0, L"反控", L"停止温控"},
		{0, L"反控", L"急停"},
		{0, L"反控", L"点火"},
		{0, L"反控", L"开空气阀"},
		{0, L"反控", L"探测此地址"},
		{0, L"反控", L"读取状态"},
		{1, L"温程", L"下载温程"},
		{2, L"PID", L"确认并写入"},
		{3, L"维护", L"设置口令"},
		{4, L"工作站", L"工作站取数"},
		{4, L"工作站", L"加载 DLL"},
		{-1, L"分页", L"反控"},
		{-1, L"分页", L"温程"},
		{-1, L"分页", L"PID"},
		{-1, L"分页", L"维护"},
		{-1, L"分页", L"工作站"},
	};
	int index;
	int failed = 0;
	wchar_t klass[64];
	RECT want;
	smoke_reset_log();
	want.left = 0;
	want.top = 0;
	want.right = 720;
	want.bottom = 680;
	AdjustWindowRect(&want, WS_OVERLAPPEDWINDOW, FALSE);
	ShowWindow(hwnd, SW_SHOWNA);
	MoveWindow(hwnd, 40, 40, want.right - want.left, want.bottom - want.top, TRUE);
	UpdateWindow(hwnd);
	GetClassNameW(hwnd, klass, 64);
	if (wcscmp(klass, L"ControlPanelClass") != 0) {
		smoke_line("offline-smoke: window class is not ControlPanelClass\n");
		return 1;
	}
	for (index = 0; index < (int)(sizeof need / sizeof need[0]); index++) {
		HWND child;
		RECT rect;
		RECT client;
		POINT origin;
		POINT far_corner;
		char tab_utf8[64];
		char text_utf8[64];
		char line[240];
		int page = need[index].page;
		int own_visible;
		show_page(page < 0 ? 0 : page);
		layout(hwnd);
		child = find_child_text(hwnd, need[index].text);
		wide_utf8(need[index].tab, tab_utf8, (int)sizeof tab_utf8);
		wide_utf8(need[index].text, text_utf8, (int)sizeof text_utf8);
		if (child == NULL) {
			snprintf(line, sizeof line, "offline-smoke: page=%d tab=%s control=%s reason=missing\n", page, tab_utf8, text_utf8);
			smoke_line(line);
			failed = 1;
			continue;
		}
		own_visible = (GetWindowLongW(child, GWL_STYLE) & WS_VISIBLE) != 0;
		if (!own_visible || !IsWindowVisible(child)) {
			snprintf(line, sizeof line, "offline-smoke: page=%d tab=%s control=%s reason=hidden\n", page, tab_utf8, text_utf8);
			smoke_line(line);
			failed = 1;
			continue;
		}
		GetClientRect(hwnd, &client);
		GetWindowRect(child, &rect);
		origin.x = rect.left;
		origin.y = rect.top;
		far_corner.x = rect.right;
		far_corner.y = rect.bottom;
		ScreenToClient(hwnd, &origin);
		ScreenToClient(hwnd, &far_corner);
		if (origin.x < 0 || origin.y < 0 || far_corner.x > client.right || far_corner.y > client.bottom || far_corner.x - origin.x < 40 || far_corner.y - origin.y < 18 || far_corner.x - origin.x < text_px(child) + 8) {
			snprintf(line, sizeof line, "offline-smoke: page=%d tab=%s control=%s reason=clipped x=%ld y=%ld r=%ld b=%ld client=%ld,%ld text=%d\n", page, tab_utf8, text_utf8, origin.x, origin.y, far_corner.x, far_corner.y, client.right, client.bottom, text_px(child));
			smoke_line(line);
			failed = 1;
		}
	}
	if (g_bridge_process != NULL || hs_session_net_writes(g_session) != 0) {
		smoke_line("offline-smoke: network or bridge was used\n");
		return 1;
	}
	if (failed) {
		return 1;
	}
	smoke_line("offline-smoke: ok\n");
	return 0;
}

static void on_command(int id) {
	char err[300] = "";
	if (g_smoke) {
		return;
	}
	char host[64];
	char port_text[16];
	char pass[80];
	char pass2[80];
	int64_t now = (int64_t)GetTickCount64();
	int ok_in = 0;
	int ok_det = 0;
	switch (id) {
	case 1:
		get_utf8(g_host, host, sizeof host);
		get_utf8(g_port, port_text, sizeof port_text);
		if (hs_session_connect_live(g_session, host, atoi(port_text), err, sizeof err) != 0) {
			show_result(err);
		}
		break;
	case 2:
		hs_session_disconnect(g_session);
		break;
	case 3:
		hs_session_set_sim(g_session);
		break;
	case 4:
		hs_session_set_offline(g_session);
		break;
	case 5: {
		double inlet_c = edit_number(g_inlet, &ok_in);
		double det_c = edit_number(g_det, &ok_det);
		if (!ok_in || !ok_det) {
			show_result("进样口或检测器目标为空，未发送");
			break;
		}
		hs_session_start_heat(g_session, 1, 1, 1, inlet_c, det_c, now, err, sizeof err);
		show_result(err);
		break;
	}
	case 6:
		hs_session_stop_heat(g_session, now, err, sizeof err);
		show_result(err);
		break;
	case 7:
		hs_session_estop(g_session, now, err, sizeof err);
		show_result(err);
		break;
	case 8:
		hs_session_ignite(g_session, err, sizeof err);
		show_result(err);
		break;
	case 9:
		hs_session_valves(g_session, 1, err, sizeof err);
		show_result(err);
		break;
	case 10:
		hs_session_valves(g_session, 0, err, sizeof err);
		show_result(err);
		break;
	case 11:
		hs_session_start_device_acq(g_session, err, sizeof err);
		show_result(err);
		break;
	case 12:
		hs_session_stop_device_acq(g_session, err, sizeof err);
		show_result(err);
		break;
	case 30:
		get_utf8(g_host, host, sizeof host);
		get_utf8(g_port, port_text, sizeof port_text);
		hs_session_probe(g_session, host, atoi(port_text), err, sizeof err);
		show_result(err);
		break;
	case 31:
		if (hs_session_poll(g_session) != 0) {
			show_result("读取状态失败。需要已连接，且设备返回气压等报文。");
		}
		break;
	case 40: {
		HsProgram program;
		if (read_program_edits(&program, err, sizeof err) != 0 || hs_session_set_program(g_session, &program, err, sizeof err) != 0) {
			show_result(err);
			break;
		}
		hs_session_write_program_now(g_session, err, sizeof err);
		show_result(err);
		fill_program_edits();
		break;
	}
	case 41:
		if (hs_session_read_program(g_session, err, sizeof err) != 0) {
			show_result(err);
		}
		fill_program_edits();
		break;
	case 50:
		if (hs_session_read_pids(g_session, err, sizeof err) != 0) {
			show_result(err);
		}
		fill_pid_edits();
		break;
	case 51: {
		char confirm[300];
		int row;
		wchar_t wide[400];
		for (row = 0; row < 3; row++) {
			int ok_p = 0, ok_i = 0, ok_d = 0;
			double kp = edit_number(g_pid[row][0], &ok_p);
			double ki = edit_number(g_pid[row][1], &ok_i);
			double kd = edit_number(g_pid[row][2], &ok_d);
			if (!ok_p || !ok_i || !ok_d || hs_session_set_pid(g_session, row, kp, ki, kd, err, sizeof err) != 0) {
				show_result(ok_p && ok_i && ok_d ? err : "PID 有空格，未发送");
				return;
			}
		}
		hs_session_pid_confirm_text(g_session, confirm, sizeof confirm);
		hs_utf8_to_wide(confirm, wide, 400);
		if (MessageBoxW(NULL, wide, L"确认写入这三路 PID？", MB_YESNO) != IDYES) {
			show_result("已取消，没有发送");
			break;
		}
		hs_session_write_pids_confirmed(g_session, confirm, now, err, sizeof err);
		show_result(err);
		break;
	}
	case 60:
		get_utf8(g_pass, pass, sizeof pass);
		get_utf8(g_pass2, pass2, sizeof pass2);
		hs_session_enroll(g_session, pass, pass2, now, err, sizeof err);
		show_result(err[0] ? err : "口令已设置");
		break;
	case 61:
		get_utf8(g_pass, pass, sizeof pass);
		hs_session_verify(g_session, pass, now, err, sizeof err);
		show_result(err);
		break;
	case 62:
		hs_session_lock_engineer(g_session, now);
		break;
	case 63:
		get_utf8(g_note, pass, sizeof pass);
		hs_session_save_column_note(g_session, pass, now, err, sizeof err);
		show_result(err);
		break;
	case 64:
		hs_session_device_apply_blocked(err, sizeof err);
		show_result(err);
		break;
	case 65:
		hs_session_write_setpoints(g_session, edit_number(g_cal_in, &ok_in), edit_number(g_cal_det, &ok_det), err, sizeof err);
		if (!ok_in || !ok_det) {
			show_result("校准目标为空，未发送");
		} else {
			show_result(err);
		}
		break;
	case 66:
		hs_session_reset_stopwatch(g_session);
		break;
	case 70: {
		char path[MAX_PATH];
		char cmd[MAX_PATH + 16];
		char reply[180];
		get_utf8(g_dll, path, sizeof path);
		if (path[0] == '\0') {
			show_result("请填写 DLL 路径。不会自动加载，包内没有厂商 HWSendData.dll。hwstation_ipc_repro.dll 只是可选试验，不是官方 DLL。");
			break;
		}
		if (bridge_start() != 0) {
			show_result("无法启动 hwbridge.exe");
			break;
		}
		snprintf(cmd, sizeof cmd, "LOAD\t%s\n", path);
		if (bridge_write_line(cmd, reply, sizeof reply) != 0) {
			show_result("桥接没有返回");
		} else {
			show_result(reply);
		}
		break;
	}
	case 71: {
		int checked = SendMessageW(g_hw_ok, BM_GETCHECK, 0, 0) == BST_CHECKED;
		int ms_ok = 0;
		int ms = (int)edit_number(g_hw_ms, &ms_ok);
		if (!ms_ok) {
			show_result("间隔为空");
			break;
		}
		hs_session_set_hw_interval(g_session, ms, checked, err, sizeof err);
		show_result(err);
		break;
	}
	case 72: {
		HsTriple triple;
		char cmd[64];
		char reply[160];
		if (hs_session_hw_start(g_session, &triple, err, sizeof err) != 0) {
			show_result(err);
			break;
		}
		if (!g_bridge_process) {
			show_result("三元组已组好，但没有加载 DLL，所以没有向窗口发送。");
			break;
		}
		snprintf(cmd, sizeof cmd, "SEND\t%d\t%d\t%d\n", triple.data1, triple.data2, triple.data3);
		if (bridge_write_line(cmd, reply, sizeof reply) != 0) {
			show_result("桥接没有返回");
		} else {
			show_result(reply);
		}
		break;
	}
	case 73:
		hs_session_hw_stop(g_session, err, sizeof err);
		show_result(err);
		break;
	case 74:
		hs_session_hw_zero(g_session, err, sizeof err);
		show_result(err);
		break;
	case 80:
	case 81:
	case 82:
	case 83:
	case 84:
		show_page(id - 80);
		break;
	default:
		break;
	}
}

static int bridge_write_line(const char *cmd, char *reply, int cap) {
	DWORD wrote = 0;
	if (!g_bridge_in || !WriteFile(g_bridge_in, cmd, (DWORD)strlen(cmd), &wrote, NULL)) {
		return -1;
	}
	return bridge_read_line(reply, cap);
}

static void layout(HWND hwnd) {
	RECT rect;
	int width;
	int mins[4] = {72, 72, 72, 88};
	int cols[4];
	int i;
	int content_top;
	HsRect nav_rects[5];
	int nav_rows;
	GetClientRect(hwnd, &rect);
	width = rect.right > 360 ? rect.right : 360;
	nav_rows = hs_layout_buttons(nav_rects, 5, 8, 4, width - 16, 72, 28, 6);
	if (nav_rows > 0) {
		for (i = 0; i < 5; i++) {
			if (g_nav[i]) {
				MoveWindow(g_nav[i], nav_rects[i].x, nav_rects[i].y, nav_rects[i].w, nav_rects[i].h, TRUE);
			}
		}
		content_top = 8 + nav_rows * 34;
	} else {
		content_top = 40;
	}
	MoveWindow(g_banner, 8, content_top, width - 16, 36, TRUE);
	content_top += 40;
	if (hs_layout_columns(cols, mins, 4, width - 16)) {
		int x = 8;
		for (i = 0; i < 4; i++) {
			if (g_zones[0][i] != NULL) {
				MoveWindow(g_zones[0][i], x, content_top, cols[i], 18, TRUE);
			}
			if (g_zones[1][i]) {
				MoveWindow(g_zones[1][i], x, content_top + 20, cols[i], 18, TRUE);
			}
			if (g_zones[2][i]) {
				MoveWindow(g_zones[2][i], x, content_top + 40, cols[i], 18, TRUE);
			}
			x += cols[i];
		}
	}
	place_flow(g_page, 8, content_top + 64, width - 16);
	if (g_page == 3 && g_trend && rect.bottom > 240) {
		MoveWindow(g_events, 8, rect.bottom - 220, width - 16, 60, TRUE);
		MoveWindow(g_trend, 8, rect.bottom - 150, width - 16, 70, TRUE);
	}
	MoveWindow(g_link, 8, rect.bottom - 88, width - 16, 20, TRUE);
	MoveWindow(g_heat, 8, rect.bottom - 66, width / 2 - 8, 20, TRUE);
	MoveWindow(g_stop, width / 2, rect.bottom - 66, width / 2 - 8, 20, TRUE);
	MoveWindow(g_acq, 8, rect.bottom - 44, width - 16, 20, TRUE);
	MoveWindow(g_alarm, 8, rect.bottom - 22, width - 16, 20, TRUE);
}

static LRESULT CALLBACK wndproc(HWND hwnd, UINT msg, WPARAM wparam, LPARAM lparam) {
	switch (msg) {
	case WM_SIZE:
		layout(hwnd);
		return 0;
	case WM_TIMER:
		refresh(hwnd);
		return 0;
	case WM_COMMAND:
		on_command(LOWORD(wparam));
		refresh(hwnd);
		layout(hwnd);
		return 0;
	case WM_CLOSE:
		MessageBoxW(hwnd, L"关闭本窗口不会向仪器发送停止。", L"反控", MB_OK);
		DestroyWindow(hwnd);
		return 0;
	case WM_DESTROY:
		KillTimer(hwnd, 1);
		PostQuitMessage(0);
		return 0;
	default:
		return DefWindowProcW(hwnd, msg, wparam, lparam);
	}
}

int WINAPI wWinMain(HINSTANCE instance, HINSTANCE prev, PWSTR cmd, int show) {
	WNDCLASSW klass;
	HWND hwnd;
	HANDLE mutex = NULL;
	wchar_t root[MAX_PATH];
	wchar_t dir[MAX_PATH];
	char dir_utf8[MAX_PATH];
	const wchar_t *command_line;
	int stage;
	int row;
	(void)prev;
	(void)cmd;
	command_line = GetCommandLineW();
	g_smoke = command_line != NULL && (wcsstr(command_line, L"--offline-smoke") != NULL || wcsstr(command_line, L"--self-test") != NULL);
	{
		HMODULE user32 = GetModuleHandleW(L"user32.dll");
		typedef BOOL(WINAPI * set_dpi_fn)(HANDLE);
		set_dpi_fn set_dpi = NULL;
		FARPROC raw = user32 ? GetProcAddress(user32, "SetProcessDpiAwarenessContext") : NULL;
		if (raw != NULL) {
			memcpy(&set_dpi, &raw, sizeof set_dpi);
			set_dpi((HANDLE)(INT_PTR)-4);
		}
	}
	if (!g_smoke) {
		mutex = CreateMutexW(NULL, TRUE, L"Local\\Hwstation.ControlPanelClass.instance");
		if (mutex == NULL || GetLastError() == ERROR_ALREADY_EXISTS) {
			MessageBoxW(NULL, L"反控面板已经打开。", L"反控", MB_OK);
			return 0;
		}
	}
	GetEnvironmentVariableW(L"LOCALAPPDATA", root, MAX_PATH);
	_snwprintf(dir, MAX_PATH, L"%s\\hwstation", root);
	CreateDirectoryW(dir, NULL);
	_snwprintf(dir, MAX_PATH, L"%s\\hwstation\\run-%lu", root, GetCurrentProcessId());
	CreateDirectoryW(dir, NULL);
	WideCharToMultiByte(CP_UTF8, 0, dir, -1, dir_utf8, MAX_PATH, NULL, NULL);
	g_session = hs_session_create(dir_utf8);
	g_font = CreateFontW(-15, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY, DEFAULT_PITCH, L"Microsoft YaHei UI");
	memset(&klass, 0, sizeof klass);
	klass.lpfnWndProc = wndproc;
	klass.hInstance = instance;
	klass.hCursor = LoadCursor(NULL, IDC_ARROW);
	klass.hbrBackground = (HBRUSH)(COLOR_BTNFACE + 1);
	klass.lpszClassName = L"ControlPanelClass";
	RegisterClassW(&klass);
	klass.lpfnWndProc = trend_proc;
	klass.lpszClassName = L"HsTrend";
	RegisterClassW(&klass);
	hwnd = CreateWindowExW(0, L"ControlPanelClass", L"色谱反控", WS_OVERLAPPEDWINDOW | (g_smoke ? 0 : WS_VISIBLE), CW_USEDEFAULT, CW_USEDEFAULT, 720, 980, NULL, NULL, instance, NULL);
	{
		const wchar_t *names[] = {L"反控", L"温程", L"PID", L"维护", L"工作站"};
		int nav;
		for (nav = 0; nav < 5; nav++) {
			g_nav[nav] = CreateWindowExW(0, L"BUTTON", names[nav], WS_CHILD | WS_VISIBLE | WS_TABSTOP, 8 + nav * 92, 4, 88, 28, hwnd, (HMENU)(INT_PTR)(80 + nav), instance, NULL);
			SendMessageW(g_nav[nav], WM_SETFONT, (WPARAM)g_font, TRUE);
		}
	}
	g_banner = make_static(hwnd, -1, L"仿真");
	for (row = 0; row < 3; row++) {
		const wchar_t *names[] = {L"进样口1", L"检测器1", L"柱箱"};
		g_zones[row][0] = make_static(hwnd, -1, names[row]);
		g_zones[row][1] = make_static(hwnd, -1, L"未知");
		g_zones[row][2] = make_static(hwnd, -1, L"未知");
		g_zones[row][3] = make_static(hwnd, -1, L"停止");
	}
	g_host = make_edit(hwnd, 0, 100);
	g_port = make_edit(hwnd, 0, 101);
	g_inlet = make_edit(hwnd, 0, 102);
	g_det = make_edit(hwnd, 0, 103);
	set_utf8(g_inlet, "200");
	set_utf8(g_det, "250");
	make_button(hwnd, 0, L"连接仪器", 1);
	make_button(hwnd, 0, L"断开", 2);
	make_button(hwnd, 0, L"探测此地址", 30);
	make_button(hwnd, 0, L"读取状态", 31);
	make_button(hwnd, 0, L"仿真", 3);
	make_button(hwnd, 0, L"离线", 4);
	make_button(hwnd, 0, L"启动温控", 5);
	make_button(hwnd, 0, L"停止温控", 6);
	make_button(hwnd, 0, L"急停", 7);
	make_button(hwnd, 0, L"点火", 8);
	make_button(hwnd, 0, L"开空气阀", 9);
	make_button(hwnd, 0, L"关空气阀", 10);
	make_button(hwnd, 0, L"设备采集", 11);
	make_button(hwnd, 0, L"停设备采集", 12);
	g_prog_max = make_edit(hwnd, 1, 200);
	g_prog_eq = make_edit(hwnd, 1, 201);
	g_prog_stable = make_edit(hwnd, 1, 202);
	g_prog_loop = make_check(hwnd, 1, L"循环", 203);
	for (stage = 0; stage < 8; stage++) {
		g_prog[stage][0] = make_edit(hwnd, 1, 210 + stage * 3);
		g_prog[stage][1] = make_edit(hwnd, 1, 211 + stage * 3);
		g_prog[stage][2] = make_edit(hwnd, 1, 212 + stage * 3);
		set_utf8(g_prog[stage][0], "0");
		set_utf8(g_prog[stage][1], stage == 0 ? "50" : "0");
		set_utf8(g_prog[stage][2], stage == 0 ? "1" : "0");
	}
	set_utf8(g_prog_max, "450");
	set_utf8(g_prog_eq, "1");
	set_utf8(g_prog_stable, "30");
	make_button(hwnd, 1, L"读取温程", 41);
	make_button(hwnd, 1, L"下载温程", 40);
	g_prog_time = make_static(hwnd, 1, L"温程未下载");
	for (row = 0; row < 3; row++) {
		g_pid[row][0] = make_edit(hwnd, 2, 300 + row * 3);
		g_pid[row][1] = make_edit(hwnd, 2, 301 + row * 3);
		g_pid[row][2] = make_edit(hwnd, 2, 302 + row * 3);
	}
	make_button(hwnd, 2, L"读取三路PID", 50);
	make_button(hwnd, 2, L"确认并写入", 51);
	g_pid_state = make_static(hwnd, 2, L"未读取");
	g_pass = make_edit(hwnd, 3, 400);
	g_pass2 = make_edit(hwnd, 3, 401);
	SendMessageW(g_pass, EM_SETPASSWORDCHAR, (WPARAM)L'*', 0);
	SendMessageW(g_pass2, EM_SETPASSWORDCHAR, (WPARAM)L'*', 0);
	make_button(hwnd, 3, L"设置口令", 60);
	make_button(hwnd, 3, L"验证", 61);
	make_button(hwnd, 3, L"锁定", 62);
	g_note = make_edit(hwnd, 3, 402);
	make_button(hwnd, 3, L"保存本机备注", 63);
	make_button(hwnd, 3, L"下发设备配置", 64);
	g_cal_in = make_edit(hwnd, 3, 403);
	g_cal_det = make_edit(hwnd, 3, 404);
	make_button(hwnd, 3, L"下载温度设定", 65);
	g_clock = make_static(hwnd, 3, L"00:00");
	make_button(hwnd, 3, L"秒表归零", 66);
	g_events = CreateWindowExW(WS_EX_CLIENTEDGE, L"EDIT", L"", WS_CHILD | ES_MULTILINE | ES_READONLY | WS_VSCROLL, 0, 0, 200, 80, hwnd, NULL, instance, NULL);
	g_pages[3][g_page_count[3]++] = g_events;
	g_trend = CreateWindowExW(0, L"HsTrend", L"", WS_CHILD, 0, 0, 200, 80, hwnd, NULL, instance, NULL);
	g_pages[3][g_page_count[3]++] = g_trend;
	g_dll = make_edit(hwnd, 4, 500);
	make_button(hwnd, 4, L"加载 DLL", 70);
	g_hw_ms = make_edit(hwnd, 4, 501);
	g_hw_ok = make_check(hwnd, 4, L"我已核实间隔", 502);
	make_button(hwnd, 4, L"保存间隔", 71);
	make_button(hwnd, 4, L"工作站取数", 72);
	make_button(hwnd, 4, L"停工作站取数", 73);
	make_button(hwnd, 4, L"工作站归零", 74);
	g_link = make_static(hwnd, -1, L"未连接");
	g_heat = make_static(hwnd, -1, L"温控停止");
	g_stop = make_static(hwnd, -1, L"未请求");
	g_acq = make_static(hwnd, -1, L"");
	g_alarm = make_static(hwnd, -1, L"");
	show_page(0);
	layout(hwnd);
	refresh(hwnd);
	if (g_smoke) {
		int code = smoke_check(hwnd);
		DestroyWindow(hwnd);
		hs_session_free(g_session);
		return code;
	}
	SetTimer(hwnd, 1, 400, NULL);
	ShowWindow(hwnd, show);
	{
		MSG msg;
		while (GetMessageW(&msg, NULL, 0, 0) > 0) {
			TranslateMessage(&msg);
			DispatchMessageW(&msg);
		}
	}
	hs_session_free(g_session);
	if (g_bridge_process) {
		CloseHandle(g_bridge_process);
	}
	if (mutex) {
		ReleaseMutex(mutex);
		CloseHandle(mutex);
	}
	return 0;
}
