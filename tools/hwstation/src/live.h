#ifndef HWSTATION_LIVE_H
#define HWSTATION_LIVE_H

#include <stddef.h>

typedef struct HsLive HsLive;

int hs_live_attempt_count(void);
int hs_live_connect(HsLive **out, const char *host, int port, int timeout_ms, char *err, size_t err_cap);
void hs_live_close(HsLive *live);
int hs_live_write(HsLive *live, const void *buf, int len);
int hs_live_read(HsLive *live, void *buf, int cap, int timeout_ms);

#endif
