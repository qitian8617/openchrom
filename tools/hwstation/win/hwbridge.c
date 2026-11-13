#include <windows.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* 32-bit adapter. The vendor DLL is not shipped. LoadLibrary runs only after
   the parent sends LOAD with a user path. SEND is the only call into the DLL.
   Window lookup stays inside the loaded DLL. */

typedef long(__stdcall *SendFn)(long, long, long);

_Static_assert(sizeof(long) == 4, "Win32 long must be 32-bit");
_Static_assert(sizeof(void *) == 4, "hwbridge must be built as x86");

static HMODULE g_module;
static SendFn g_send;
static int g_wedged;
static char g_symbol[64];

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
	if (fseek(fp, offset, SEEK_SET) != 0 || fread(pe, 1, 6, fp) != 6) {
		fclose(fp);
		return -1;
	}
	fclose(fp);
	if (pe[0] != 'P' || pe[1] != 'E') {
		return -1;
	}
	*machine = (unsigned)pe[4] | ((unsigned)pe[5] << 8);
	return 0;
}

static DWORD WINAPI call_send(LPVOID param) {
	long *args = (long *)param;
	args[3] = g_send(args[0], args[1], args[2]);
	return 0;
}

static void handle_load(const char *path) {
	unsigned machine = 0;
	if (pe_machine(path, &machine) != 0) {
		printf("MISSING\tnot-a-pe\n");
		fflush(stdout);
		return;
	}
	if (machine != 0x14C) {
		printf("MISSING\tmachine=0x%X\tneed-i386\n", machine);
		fflush(stdout);
		return;
	}
	g_module = LoadLibraryA(path);
	if (!g_module) {
		printf("MISSING\tload-failed\t%lu\n", GetLastError());
		fflush(stdout);
		return;
	}
	g_send = (SendFn)GetProcAddress(g_module, "SendDataToHW");
	snprintf(g_symbol, sizeof g_symbol, "SendDataToHW");
	if (!g_send) {
		g_send = (SendFn)GetProcAddress(g_module, "SendDataToHW@12");
		snprintf(g_symbol, sizeof g_symbol, "SendDataToHW@12");
	}
	if (!g_send) {
		printf("MISSING\texport-not-found\n");
		fflush(stdout);
		return;
	}
	printf("LOADED\tmachine=0x14C\texport=%s\n", g_symbol);
	fflush(stdout);
}

static void handle_send(long a, long b, long c) {
	long args[4];
	HANDLE thread;
	DWORD wait;
	if (g_wedged) {
		printf("WEDGED\n");
		fflush(stdout);
		return;
	}
	if (!g_send) {
		printf("NOT_LOADED\n");
		fflush(stdout);
		return;
	}
	args[0] = a;
	args[1] = b;
	args[2] = c;
	args[3] = -1;
	thread = CreateThread(NULL, 0, call_send, args, 0, NULL);
	if (!thread) {
		printf("NOT_LOADED\n");
		fflush(stdout);
		return;
	}
	wait = WaitForSingleObject(thread, 2000);
	if (wait != WAIT_OBJECT_0) {
		g_wedged = 1;
		printf("WEDGED\ttimeout\n");
		fflush(stdout);
		return;
	}
	CloseHandle(thread);
	printf("DLL\t%ld\n", args[3]);
	fflush(stdout);
}

int main(void) {
	char line[1200];
	setvbuf(stdout, NULL, _IONBF, 0);
	printf("READY\n");
	while (fgets(line, sizeof line, stdin)) {
		if (strncmp(line, "QUIT", 4) == 0) {
			break;
		}
		if (strncmp(line, "LOAD\t", 5) == 0) {
			char *path = line + 5;
			char *nl = strchr(path, '\n');
			if (nl) {
				*nl = '\0';
			}
			nl = strchr(path, '\r');
			if (nl) {
				*nl = '\0';
			}
			handle_load(path);
			continue;
		}
		if (strncmp(line, "SEND\t", 5) == 0) {
			long a = 0, b = 0, c = 0;
			if (sscanf(line + 5, "%ld\t%ld\t%ld", &a, &b, &c) != 3) {
				printf("BAD\n");
				fflush(stdout);
				continue;
			}
			handle_send(a, b, c);
			continue;
		}
		printf("BAD\n");
		fflush(stdout);
	}
	return 0;
}
