#include "live.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef _WIN32
#include <winsock2.h>
#include <ws2tcpip.h>
typedef SOCKET hs_sock;
#define HS_INVALID INVALID_SOCKET
#define hs_close_socket closesocket
#else
#include <arpa/inet.h>
#include <errno.h>
#include <fcntl.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <unistd.h>
typedef int hs_sock;
#define HS_INVALID (-1)
#define hs_close_socket close
#endif

struct HsLive {
	hs_sock fd;
	int writes;
};

static int g_attempts;

int hs_live_attempt_count(void) {
	return g_attempts;
}

static int set_timeout(hs_sock fd, int timeout_ms) {
#ifdef _WIN32
	DWORD tv = (DWORD)timeout_ms;
	return setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, (const char *)&tv, sizeof tv);
#else
	struct timeval tv;
	tv.tv_sec = timeout_ms / 1000;
	tv.tv_usec = (timeout_ms % 1000) * 1000;
	if (setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof tv) != 0) {
		return -1;
	}
	return setsockopt(fd, SOL_SOCKET, SO_SNDTIMEO, &tv, sizeof tv);
#endif
}

int hs_live_connect(HsLive **out, const char *host, int port, int timeout_ms, char *err, size_t err_cap) {
	hs_sock fd;
	struct sockaddr_in addr;
#ifdef _WIN32
	static int started;
	WSADATA data;
	if (!started) {
		if (WSAStartup(MAKEWORD(2, 2), &data) != 0) {
			snprintf(err, err_cap, "Winsock 初始化失败");
			return -1;
		}
		started = 1;
	}
#endif
	if (out == NULL || host == NULL || host[0] == '\0' || port <= 0 || port > 65535) {
		if (err && err_cap) {
			snprintf(err, err_cap, "拒绝连接：主机或端口无效");
		}
		return -1;
	}
	g_attempts++;
	fd = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
	if (fd == HS_INVALID) {
		if (err && err_cap) {
			snprintf(err, err_cap, "无法创建套接字");
		}
		return -1;
	}
	memset(&addr, 0, sizeof addr);
	addr.sin_family = AF_INET;
	addr.sin_port = htons((unsigned short)port);
	if (inet_pton(AF_INET, host, &addr.sin_addr) != 1) {
		hs_close_socket(fd);
		if (err && err_cap) {
			snprintf(err, err_cap, "只接受显式 IPv4 地址，不解析主机名，也不猜测地址");
		}
		return -1;
	}
	set_timeout(fd, timeout_ms > 0 ? timeout_ms : 3000);
	if (connect(fd, (struct sockaddr *)&addr, sizeof addr) != 0) {
		hs_close_socket(fd);
		if (err && err_cap) {
			snprintf(err, err_cap, "连接失败");
		}
		return -1;
	}
	*out = calloc(1, sizeof **out);
	if (*out == NULL) {
		hs_close_socket(fd);
		return -1;
	}
	(*out)->fd = fd;
	return 0;
}

void hs_live_close(HsLive *live) {
	if (live == NULL) {
		return;
	}
	if (live->fd != HS_INVALID) {
		hs_close_socket(live->fd);
	}
	free(live);
}

int hs_live_write(HsLive *live, const void *buf, int len) {
	int sent;
	if (live == NULL || buf == NULL || len < 0) {
		return -1;
	}
#ifdef _WIN32
	sent = send(live->fd, (const char *)buf, len, 0);
#else
	sent = (int)send(live->fd, buf, (size_t)len, 0);
#endif
	if (sent > 0) {
		live->writes += sent;
	}
	return sent;
}

int hs_live_read(HsLive *live, void *buf, int cap, int timeout_ms) {
	(void)timeout_ms;
	if (live == NULL || buf == NULL || cap <= 0) {
		return -1;
	}
#ifdef _WIN32
	return recv(live->fd, (char *)buf, cap, 0);
#else
	return (int)recv(live->fd, buf, (size_t)cap, 0);
#endif
}
