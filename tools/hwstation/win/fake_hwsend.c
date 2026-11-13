#include <windows.h>
#include <stdio.h>
/* Test double. Does not call FindWindow and does not know HWClass. */
long __stdcall SendDataToHW(long a, long b, long c) {
    char path[MAX_PATH];
    DWORD n = GetEnvironmentVariableA("HWSTATION_FAKE_LOG", path, MAX_PATH);
    if (n > 0 && n < MAX_PATH) {
        FILE *fp = fopen(path, "ab");
        if (fp) {
            fprintf(fp, "%ld %ld %ld\n", a, b, c);
            fclose(fp);
        }
    }
    return 1;
}
long __stdcall HwstationFakeBuild(void) { return 20261113; }
