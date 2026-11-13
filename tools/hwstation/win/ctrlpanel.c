#include "hs.h"

#include <windows.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define IDC_HOST 100
#define IDC_PORT 101
#define IDC_INLET 102
#define IDC_DET 103
#define IDC_STATUS 104
#define IDC_ALARM 105
#define IDC_LINK 106
#define IDC_HEAT 107
#define IDC_STOP 108
#define IDC_ACQ 109
#define IDC_HW 110
#define IDC_PASS 111
#define IDC_DLL 112

static HsSession *g_session;
static HWND g_buttons[24];
static int g_button_count;
static HFONT g_font;
static HWND g_host, g_port, g_inlet, g_det, g_pass, g_dll;
static HWND g_link, g_heat, g_stop, g_acq, g_hw, g_alarm, g_banner;
static HWND g_zones[3][4];
static wchar_t g_dir[MAX_PATH];
static HANDLE g_bridge_in;
static HANDLE g_bridge_out;
static HANDLE g_bridge_process;

static void set_utf8(HWND hwnd, const char *text) {
	wchar_t wide[400];
	if (!hwnd) {
		return;
	}
	if (hs_utf8_to_wide(text ? text : "", wide, 400) < 0) {
		SetWindowTextW(hwnd, L"?");
		return;
	}
	SetWindowTextW(hwnd, wide);
}

static void get_utf8(HWND hwnd, char *out, size_t cap) {
	wchar_t wide[256];
	int n;
	size_t i;
	size_t o = 0;
	GetWindowTextW(hwnd, wide, 256);
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
	(void)i;
}

static HWND add_button(HWND parent, const wchar_t *text, int id) {
	HWND hwnd = CreateWindowExW(0, L"BUTTON", text, WS_CHILD | WS_VISIBLE | WS_TABSTOP, 0, 0, 80, 28, parent, (HMENU)(INT_PTR)id, GetModuleHandleW(NULL), NULL);
	SendMessageW(hwnd, WM_SETFONT, (WPARAM)g_font, TRUE);
	if (g_button_count < 24) {
		g_buttons[g_button_count++] = hwnd;
	}
	return hwnd;
}

static HWND add_static(HWND parent, const wchar_t *text, int id) {
	HWND hwnd = CreateWindowExW(0, L"STATIC", text, WS_CHILD | WS_VISIBLE, 0, 0, 80, 22, parent, (HMENU)(INT_PTR)id, GetModuleHandleW(NULL), NULL);
	SendMessageW(hwnd, WM_SETFONT, (WPARAM)g_font, TRUE);
	return hwnd;
}

static HWND add_edit(HWND parent, int id, int password) {
	HWND hwnd = CreateWindowExW(WS_EX_CLIENTEDGE, L"EDIT", L"", WS_CHILD | WS_VISIBLE | ES_AUTOHSCROLL | (password ? ES_PASSWORD : 0), 0, 0, 80, 24, parent, (HMENU)(INT_PTR)id, GetModuleHandleW(NULL), NULL);
	SendMessageW(hwnd, WM_SETFONT, (WPARAM)g_font, TRUE);
	return hwnd;
}

static void refresh(HWND hwnd) {
	HsSnapshot snap;
	int64_t now = GetTickCount64();
	char line[240];
	(void)hwnd;
	hs_session_tick(g_session, (int64_t)now);
	hs_session_snapshot(g_session, (int64_t)now, &snap);
	set_utf8(g_link, snap.link);
	set_utf8(g_heat, snap.heat);
	set_utf8(g_stop, snap.stop_confirm);
	snprintf(line, sizeof line, "%s | %s", snap.device_acq, snap.hw);
	set_utf8(g_acq, line);
	snprintf(line, sizeof line, "火焰 %s", snap.flame);
	set_utf8(g_hw, line);
	snprintf(line, sizeof line, "%s | H2 %s | 空气 %s | %s", snap.banner[0] ? snap.banner : snap.mode, snap.h2, snap.air, snap.engineer);
	set_utf8(g_banner, line);
	if (snap.alarm_on) {
		set_utf8(g_alarm, snap.alarm);
	} else {
		set_utf8(g_alarm, "无异常");
	}
	set_utf8(g_zones[0][0], "进样口1");
	set_utf8(g_zones[0][1], snap.inlet_set);
	set_utf8(g_zones[0][2], snap.inlet_act);
	set_utf8(g_zones[0][3], snap.inlet_state);
	set_utf8(g_zones[1][0], "检测器1");
	set_utf8(g_zones[1][1], snap.det_set);
	set_utf8(g_zones[1][2], snap.det_act);
	set_utf8(g_zones[1][3], snap.det_state);
	set_utf8(g_zones[2][0], "柱箱");
	set_utf8(g_zones[2][1], snap.oven_set);
	set_utf8(g_zones[2][2], snap.oven_act);
	set_utf8(g_zones[2][3], snap.oven_state);
}

static void layout(HWND hwnd) {
	RECT rect;
	int width;
	int mins[4] = {72, 72, 72, 88};
	int cols[4];
	HsRect buttons[24];
	int i;
	int top;
	GetClientRect(hwnd, &rect);
	width = rect.right - rect.left;
	if (width < 320) {
		width = 320;
	}
	MoveWindow(g_banner, 8, 4, width - 16, 22, TRUE);
	if (!hs_layout_columns(cols, mins, 4, width - 16)) {
		return;
	}
	top = 28;
	{
		int x = 8;
		const wchar_t *heads[] = {L"通道", L"目标°C", L"当前°C", L"状态"};
		for (i = 0; i < 4; i++) {
			HWND head = GetDlgItem(hwnd, 200 + i);
			if (head) {
				SetWindowTextW(head, heads[i]);
				MoveWindow(head, x, top, cols[i], 20, TRUE);
			}
			x += cols[i];
		}
	}
	for (i = 0; i < 3; i++) {
		int x = 8;
		int col;
		for (col = 0; col < 4; col++) {
			MoveWindow(g_zones[i][col], x, top + 22 + i * 22, cols[col], 20, TRUE);
			x += cols[col];
		}
	}
	MoveWindow(g_host, 8, 120, 180, 24, TRUE);
	MoveWindow(g_port, 196, 120, 80, 24, TRUE);
	MoveWindow(g_inlet, 284, 120, 70, 24, TRUE);
	MoveWindow(g_det, 360, 120, 70, 24, TRUE);
	if (hs_layout_buttons(buttons, g_button_count, 8, 154, width - 16, 88, 30, 6) > 0) {
		for (i = 0; i < g_button_count; i++) {
			MoveWindow(g_buttons[i], buttons[i].x, buttons[i].y, buttons[i].w, buttons[i].h, TRUE);
		}
		top = buttons[g_button_count - 1].y + buttons[g_button_count - 1].h + 8;
	} else {
		top = 400;
	}
	MoveWindow(g_pass, 8, top, 160, 24, TRUE);
	MoveWindow(g_dll, 176, top, width - 190, 24, TRUE);
	MoveWindow(g_link, 8, rect.bottom - 92, width - 16, 18, TRUE);
	MoveWindow(g_heat, 8, rect.bottom - 74, width / 2 - 12, 18, TRUE);
	MoveWindow(g_stop, width / 2, rect.bottom - 74, width / 2 - 12, 18, TRUE);
	MoveWindow(g_acq, 8, rect.bottom - 56, width - 16, 18, TRUE);
	MoveWindow(g_hw, 8, rect.bottom - 38, 120, 18, TRUE);
	MoveWindow(g_alarm, 132, rect.bottom - 38, width - 140, 18, TRUE);
}

static double edit_number(HWND hwnd, double fallback) {
	char text[64];
	char *end = NULL;
	double value;
	get_utf8(hwnd, text, sizeof text);
	if (text[0] == '\0') {
		return fallback;
	}
	value = strtod(text, &end);
	if (end == text) {
		return fallback;
	}
	return value;
}

static void show_result(const char *text) {
	wchar_t wide[400];
	hs_utf8_to_wide(text ? text : "", wide, 400);
	MessageBoxW(NULL, wide, L"反控", MB_OK);
}

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

static int bridge_write(const char *text) {
	DWORD wrote = 0;
	if (!g_bridge_in || !WriteFile(g_bridge_in, text, (DWORD)strlen(text), &wrote, NULL)) {
		return -1;
	}
	return 0;
}

static int bridge_start(void) {
	wchar_t exe[MAX_PATH];
	wchar_t bridge[MAX_PATH];
	SECURITY_ATTRIBUTES sa;
	STARTUPINFOW si;
	PROCESS_INFORMATION pi;
	HANDLE read_out = NULL;
	HANDLE write_out = NULL;
	HANDLE read_in = NULL;
	HANDLE write_in = NULL;
	char ready[64];
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
	sa.nLength = sizeof sa;
	sa.bInheritHandle = TRUE;
	if (!CreatePipe(&read_out, &write_out, &sa, 0) || !CreatePipe(&read_in, &write_in, &sa, 0)) {
		return -1;
	}
	SetHandleInformation(read_in, HANDLE_FLAG_INHERIT, 0);
	SetHandleInformation(write_out, HANDLE_FLAG_INHERIT, 0);
	memset(&si, 0, sizeof si);
	si.cb = sizeof si;
	si.dwFlags = STARTF_USESTDHANDLES;
	si.hStdInput = read_in;
	si.hStdOutput = write_out;
	si.hStdError = write_out;
	memset(&pi, 0, sizeof pi);
	if (!CreateProcessW(bridge, NULL, NULL, NULL, TRUE, CREATE_NO_WINDOW, NULL, NULL, &si, &pi)) {
		CloseHandle(read_out);
		CloseHandle(write_out);
		CloseHandle(read_in);
		CloseHandle(write_in);
		return -1;
	}
	CloseHandle(read_in);
	CloseHandle(write_out);
	CloseHandle(pi.hThread);
	g_bridge_in = write_in;
	g_bridge_out = read_out;
	g_bridge_process = pi.hProcess;
	if (bridge_read_line(ready, sizeof ready) != 0 || strcmp(ready, "READY") != 0) {
		return -1;
	}
	return 0;
}

static void on_command(int id) {
	char err[256];
	char host[64];
	char port_text[32];
	char pass[80];
	int64_t now = (int64_t)GetTickCount64();
	err[0] = '\0';
	switch (id) {
	case 1:
		get_utf8(g_host, host, sizeof host);
		get_utf8(g_port, port_text, sizeof port_text);
		if (hs_session_connect_live(g_session, host, atoi(port_text), err, sizeof err) != 0) {
			show_result(err[0] ? err : "未连接");
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
	case 5:
		hs_session_start_heat(g_session, 1, 1, 1, edit_number(g_inlet, 200), edit_number(g_det, 250), now, err, sizeof err);
		show_result(err);
		break;
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
	case 13: {
		HsTriple triple;
		char cmd[80];
		char reply[160];
		if (hs_session_hw_start(g_session, &triple, err, sizeof err) != 0) {
			show_result(err);
			break;
		}
		if (!g_bridge_process) {
			show_result("三元组已组好，但还没有加载 DLL，所以没有向任何窗口发送。");
			break;
		}
		snprintf(cmd, sizeof cmd, "SEND\t%d\t%d\t%d\n", triple.data1, triple.data2, triple.data3);
		if (bridge_write(cmd) != 0 || bridge_read_line(reply, sizeof reply) != 0) {
			show_result("桥接没有返回");
			break;
		}
		show_result(reply);
		break;
	}
	case 14:
		hs_session_hw_stop(g_session, err, sizeof err);
		show_result(err);
		break;
	case 15:
		hs_session_hw_zero(g_session, err, sizeof err);
		show_result(err);
		break;
	case 16: {
		char path[MAX_PATH];
		char cmd[MAX_PATH + 16];
		char reply[200];
		get_utf8(g_dll, path, sizeof path);
		if (path[0] == '\0') {
			show_result("请先填写 DLL 路径。不会自动加载，目录里也没有厂商 HWSendData.dll。");
			break;
		}
		if (bridge_start() != 0) {
			show_result("无法启动旁边的 hwbridge.exe。");
			break;
		}
		snprintf(cmd, sizeof cmd, "LOAD\t%s\n", path);
		if (bridge_write(cmd) != 0 || bridge_read_line(reply, sizeof reply) != 0) {
			show_result("桥接没有返回加载结果");
			break;
		}
		show_result(reply);
		break;
	}
	case 18: {
		char confirm[80];
		get_utf8(g_pass, pass, sizeof pass);
		get_utf8(g_pass, confirm, sizeof confirm);
		hs_session_verify(g_session, pass, now, err, sizeof err);
		show_result(err);
		break;
	}
	case 19:
		hs_session_lock_engineer(g_session, now);
		break;
	case 20:
		hs_session_write_pid(g_session, 0, 1, 0, 0, now, err, sizeof err);
		show_result(err);
		break;
	default:
		break;
	}
}

static LRESULT CALLBACK wndproc(HWND hwnd, UINT msg, WPARAM wparam, LPARAM lparam) {
	switch (msg) {
	case WM_CREATE:
		layout(hwnd);
		SetTimer(hwnd, 1, 200, NULL);
		return 0;
	case WM_SIZE:
		layout(hwnd);
		return 0;
	case WM_TIMER:
		refresh(hwnd);
		return 0;
	case WM_COMMAND:
		on_command(LOWORD(wparam));
		refresh(hwnd);
		return 0;
	case WM_CLOSE:
		MessageBoxW(hwnd, L"关闭本窗口不会向仪器发送停止。需要停止请先点停止或急停。", L"反控", MB_OK);
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
	MSG msg;
	HANDLE mutex;
	wchar_t dir[MAX_PATH];
	char dir_utf8[MAX_PATH];
	DWORD pid;
	int i;
	(void)prev;
	(void)cmd;
	mutex = CreateMutexW(NULL, TRUE, L"Local\\Hwstation.ControlPanelClass.instance");
	if (mutex == NULL || GetLastError() == ERROR_ALREADY_EXISTS) {
		MessageBoxW(NULL, L"反控面板已经打开。第二个进程不会写同一份日志。", L"反控", MB_OK);
		return 0;
	}
	pid = GetCurrentProcessId();
	GetEnvironmentVariableW(L"LOCALAPPDATA", dir, MAX_PATH);
	_snwprintf(g_dir, MAX_PATH, L"%s\\hwstation\\run-%lu", dir, pid);
	CreateDirectoryW(L"hwstation", NULL);
	CreateDirectoryW(g_dir, NULL);
	WideCharToMultiByte(CP_UTF8, 0, g_dir, -1, dir_utf8, MAX_PATH, NULL, NULL);
	g_session = hs_session_create(dir_utf8);
	g_font = CreateFontW(-16, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY, DEFAULT_PITCH, L"Microsoft YaHei UI");
	memset(&klass, 0, sizeof klass);
	klass.lpfnWndProc = wndproc;
	klass.hInstance = instance;
	klass.hCursor = LoadCursor(NULL, IDC_ARROW);
	klass.hbrBackground = (HBRUSH)(COLOR_BTNFACE + 1);
	klass.lpszClassName = L"ControlPanelClass";
	RegisterClassW(&klass);
	hwnd = CreateWindowExW(0, L"ControlPanelClass", L"色谱反控", WS_OVERLAPPEDWINDOW | WS_VISIBLE, CW_USEDEFAULT, CW_USEDEFAULT, 600, 960, NULL, NULL, instance, NULL);
	g_banner = add_static(hwnd, L"仿真", IDC_STATUS);
	for (i = 0; i < 4; i++) {
		CreateWindowExW(0, L"STATIC", L"", WS_CHILD | WS_VISIBLE, 0, 0, 10, 10, hwnd, (HMENU)(INT_PTR)(200 + i), instance, NULL);
	}
	for (i = 0; i < 3; i++) {
		int col;
		for (col = 0; col < 4; col++) {
			g_zones[i][col] = add_static(hwnd, L"--", 0);
		}
	}
	g_host = add_edit(hwnd, IDC_HOST, 0);
	g_port = add_edit(hwnd, IDC_PORT, 0);
	g_inlet = add_edit(hwnd, IDC_INLET, 0);
	g_det = add_edit(hwnd, IDC_DET, 0);
	SetWindowTextW(g_inlet, L"200");
	SetWindowTextW(g_det, L"250");
	add_button(hwnd, L"连接仪器", 1);
	add_button(hwnd, L"断开", 2);
	add_button(hwnd, L"仿真", 3);
	add_button(hwnd, L"离线", 4);
	add_button(hwnd, L"启动温控", 5);
	add_button(hwnd, L"停止温控", 6);
	add_button(hwnd, L"急停", 7);
	add_button(hwnd, L"点火", 8);
	add_button(hwnd, L"开空气阀", 9);
	add_button(hwnd, L"关空气阀", 10);
	add_button(hwnd, L"设备采集", 11);
	add_button(hwnd, L"停设备采集", 12);
	add_button(hwnd, L"工作站取数", 13);
	add_button(hwnd, L"停工作站取数", 14);
	add_button(hwnd, L"工作站归零", 15);
	add_button(hwnd, L"加载 DLL", 16);
	add_button(hwnd, L"验证维护", 18);
	add_button(hwnd, L"锁定维护", 19);
	add_button(hwnd, L"写柱箱PID", 20);
	g_pass = add_edit(hwnd, IDC_PASS, 1);
	g_dll = add_edit(hwnd, IDC_DLL, 0);
	g_link = add_static(hwnd, L"", IDC_LINK);
	g_heat = add_static(hwnd, L"", IDC_HEAT);
	g_stop = add_static(hwnd, L"", IDC_STOP);
	g_acq = add_static(hwnd, L"", IDC_ACQ);
	g_hw = add_static(hwnd, L"", IDC_HW);
	g_alarm = add_static(hwnd, L"", IDC_ALARM);
	layout(hwnd);
	refresh(hwnd);
	ShowWindow(hwnd, show);
	while (GetMessageW(&msg, NULL, 0, 0) > 0) {
		TranslateMessage(&msg);
		DispatchMessageW(&msg);
	}
	hs_session_free(g_session);
	ReleaseMutex(mutex);
	CloseHandle(mutex);
	return 0;
}
