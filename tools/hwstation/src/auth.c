#include "hs.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>

#ifdef _WIN32
#include <windows.h>
#include <wincrypt.h>
#else
#include <fcntl.h>
#include <unistd.h>
#endif

typedef struct Sha256 {
	uint32_t state[8];
	uint64_t bits;
	uint8_t buffer[64];
	size_t used;
} Sha256;

static uint32_t rotr(uint32_t x, uint32_t n) {
	return (x >> n) | (x << (32 - n));
}

static void sha256_init(Sha256 *ctx) {
	ctx->state[0] = 0x6a09e667u;
	ctx->state[1] = 0xbb67ae85u;
	ctx->state[2] = 0x3c6ef372u;
	ctx->state[3] = 0xa54ff53au;
	ctx->state[4] = 0x510e527fu;
	ctx->state[5] = 0x9b05688cu;
	ctx->state[6] = 0x1f83d9abu;
	ctx->state[7] = 0x5be0cd19u;
	ctx->bits = 0;
	ctx->used = 0;
}

static void sha256_block(Sha256 *ctx, const uint8_t block[64]) {
	static const uint32_t k[64] = {
		0x428a2f98u, 0x71374491u, 0xb5c0fbcfu, 0xe9b5dba5u, 0x3956c25bu, 0x59f111f1u, 0x923f82a4u, 0xab1c5ed5u,
		0xd807aa98u, 0x12835b01u, 0x243185beu, 0x550c7dc3u, 0x72be5d74u, 0x80deb1feu, 0x9bdc06a7u, 0xc19bf174u,
		0xe49b69c1u, 0xefbe4786u, 0x0fc19dc6u, 0x240ca1ccu, 0x2de92c6fu, 0x4a7484aau, 0x5cb0a9dcu, 0x76f988dau,
		0x983e5152u, 0xa831c66du, 0xb00327c8u, 0xbf597fc7u, 0xc6e00bf3u, 0xd5a79147u, 0x06ca6351u, 0x14292967u,
		0x27b70a85u, 0x2e1b2138u, 0x4d2c6dfcu, 0x53380d13u, 0x650a7354u, 0x766a0abbu, 0x81c2c92eu, 0x92722c85u,
		0xa2bfe8a1u, 0xa81a664bu, 0xc24b8b70u, 0xc76c51a3u, 0xd192e819u, 0xd6990624u, 0xf40e3585u, 0x106aa070u,
		0x19a4c116u, 0x1e376c08u, 0x2748774cu, 0x34b0bcb5u, 0x391c0cb3u, 0x4ed8aa4au, 0x5b9cca4fu, 0x682e6ff3u,
		0x748f82eeu, 0x78a5636fu, 0x84c87814u, 0x8cc70208u, 0x90befffau, 0xa4506cebu, 0xbef9a3f7u, 0xc67178f2u};
	uint32_t w[64];
	uint32_t a, b, c, d, e, f, g, h;
	int i;
	for (i = 0; i < 16; i++) {
		w[i] = ((uint32_t)block[i * 4] << 24) | ((uint32_t)block[i * 4 + 1] << 16) | ((uint32_t)block[i * 4 + 2] << 8) | block[i * 4 + 3];
	}
	for (i = 16; i < 64; i++) {
		uint32_t s0 = rotr(w[i - 15], 7) ^ rotr(w[i - 15], 18) ^ (w[i - 15] >> 3);
		uint32_t s1 = rotr(w[i - 2], 17) ^ rotr(w[i - 2], 19) ^ (w[i - 2] >> 10);
		w[i] = w[i - 16] + s0 + w[i - 7] + s1;
	}
	a = ctx->state[0];
	b = ctx->state[1];
	c = ctx->state[2];
	d = ctx->state[3];
	e = ctx->state[4];
	f = ctx->state[5];
	g = ctx->state[6];
	h = ctx->state[7];
	for (i = 0; i < 64; i++) {
		uint32_t S1 = rotr(e, 6) ^ rotr(e, 11) ^ rotr(e, 25);
		uint32_t ch = (e & f) ^ ((~e) & g);
		uint32_t t1 = h + S1 + ch + k[i] + w[i];
		uint32_t S0 = rotr(a, 2) ^ rotr(a, 13) ^ rotr(a, 22);
		uint32_t maj = (a & b) ^ (a & c) ^ (b & c);
		uint32_t t2 = S0 + maj;
		h = g;
		g = f;
		f = e;
		e = d + t1;
		d = c;
		c = b;
		b = a;
		a = t1 + t2;
	}
	ctx->state[0] += a;
	ctx->state[1] += b;
	ctx->state[2] += c;
	ctx->state[3] += d;
	ctx->state[4] += e;
	ctx->state[5] += f;
	ctx->state[6] += g;
	ctx->state[7] += h;
}

static void sha256_update(Sha256 *ctx, const uint8_t *data, size_t len) {
	size_t i = 0;
	while (i < len) {
		size_t room = 64 - ctx->used;
		size_t take = len - i < room ? len - i : room;
		memcpy(ctx->buffer + ctx->used, data + i, take);
		ctx->used += take;
		i += take;
		if (ctx->used == 64) {
			sha256_block(ctx, ctx->buffer);
			ctx->bits += 512;
			ctx->used = 0;
		}
	}
}

static void sha256_final(Sha256 *ctx, uint8_t out[32]) {
	size_t i;
	ctx->bits += (uint64_t)ctx->used * 8u;
	ctx->buffer[ctx->used++] = 0x80;
	if (ctx->used > 56) {
		while (ctx->used < 64) {
			ctx->buffer[ctx->used++] = 0;
		}
		sha256_block(ctx, ctx->buffer);
		ctx->used = 0;
	}
	while (ctx->used < 56) {
		ctx->buffer[ctx->used++] = 0;
	}
	for (i = 0; i < 8; i++) {
		ctx->buffer[63 - i] = (uint8_t)(ctx->bits >> (8 * i));
	}
	sha256_block(ctx, ctx->buffer);
	for (i = 0; i < 8; i++) {
		out[i * 4] = (uint8_t)(ctx->state[i] >> 24);
		out[i * 4 + 1] = (uint8_t)(ctx->state[i] >> 16);
		out[i * 4 + 2] = (uint8_t)(ctx->state[i] >> 8);
		out[i * 4 + 3] = (uint8_t)ctx->state[i];
	}
}

static void hmac_sha256(const uint8_t *key, size_t key_len, const uint8_t *data, size_t data_len, uint8_t out[32]) {
	uint8_t k[64];
	uint8_t ipad[64];
	uint8_t opad[64];
	uint8_t inner[32];
	Sha256 ctx;
	size_t i;
	memset(k, 0, sizeof k);
	if (key_len > 64) {
		sha256_init(&ctx);
		sha256_update(&ctx, key, key_len);
		sha256_final(&ctx, k);
	} else {
		memcpy(k, key, key_len);
	}
	for (i = 0; i < 64; i++) {
		ipad[i] = (uint8_t)(k[i] ^ 0x36);
		opad[i] = (uint8_t)(k[i] ^ 0x5c);
	}
	sha256_init(&ctx);
	sha256_update(&ctx, ipad, 64);
	sha256_update(&ctx, data, data_len);
	sha256_final(&ctx, inner);
	sha256_init(&ctx);
	sha256_update(&ctx, opad, 64);
	sha256_update(&ctx, inner, 32);
	sha256_final(&ctx, out);
}

int hs_pbkdf2_sha256(const uint8_t *password, size_t password_len, const uint8_t *salt, size_t salt_len, int iterations, uint8_t out[32]) {
	uint8_t u[32];
	uint8_t t[32];
	uint8_t block[128];
	int i;
	int n;
	if (password == NULL || salt == NULL || out == NULL || iterations < 1 || salt_len > 120) {
		return -1;
	}
	memcpy(block, salt, salt_len);
	block[salt_len] = 0;
	block[salt_len + 1] = 0;
	block[salt_len + 2] = 0;
	block[salt_len + 3] = 1;
	hmac_sha256(password, password_len, block, salt_len + 4, u);
	memcpy(t, u, 32);
	for (i = 1; i < iterations; i++) {
		hmac_sha256(password, password_len, u, 32, u);
		for (n = 0; n < 32; n++) {
			t[n] ^= u[n];
		}
	}
	memcpy(out, t, 32);
	return 0;
}

static const char b64tab[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

static int b64_encode(const uint8_t *in, size_t len, char *out, size_t cap) {
	size_t i;
	size_t n = 0;
	for (i = 0; i < len; i += 3) {
		unsigned v = in[i] << 16;
		int pad = 0;
		if (i + 1 < len) {
			v |= in[i + 1] << 8;
		} else {
			pad++;
		}
		if (i + 2 < len) {
			v |= in[i + 2];
		} else {
			pad++;
		}
		if (n + 4 >= cap) {
			return -1;
		}
		out[n++] = b64tab[(v >> 18) & 63];
		out[n++] = b64tab[(v >> 12) & 63];
		out[n++] = pad == 2 ? '=' : b64tab[(v >> 6) & 63];
		out[n++] = pad >= 1 ? '=' : b64tab[v & 63];
	}
	out[n] = '\0';
	return (int)n;
}

static int b64_val(int c) {
	if (c >= 'A' && c <= 'Z') return c - 'A';
	if (c >= 'a' && c <= 'z') return c - 'a' + 26;
	if (c >= '0' && c <= '9') return c - '0' + 52;
	if (c == '+') return 62;
	if (c == '/') return 63;
	return -1;
}

static int b64_decode(const char *in, uint8_t *out, size_t cap) {
	size_t n = 0;
	int val[4];
	int k = 0;
	for (; *in && *in != '\n' && *in != '\r'; in++) {
		int v;
		if (*in == '=') {
			break;
		}
		v = b64_val((unsigned char)*in);
		if (v < 0) {
			return -1;
		}
		val[k++] = v;
		if (k == 4) {
			if (n + 3 > cap) {
				return -1;
			}
			out[n++] = (uint8_t)((val[0] << 2) | (val[1] >> 4));
			out[n++] = (uint8_t)((val[1] << 4) | (val[2] >> 2));
			out[n++] = (uint8_t)((val[2] << 6) | val[3]);
			k = 0;
		}
	}
	if (k == 2) {
		if (n + 1 > cap) {
			return -1;
		}
		out[n++] = (uint8_t)((val[0] << 2) | (val[1] >> 4));
	} else if (k == 3) {
		if (n + 2 > cap) {
			return -1;
		}
		out[n++] = (uint8_t)((val[0] << 2) | (val[1] >> 4));
		out[n++] = (uint8_t)((val[1] << 4) | (val[2] >> 2));
	} else if (k != 0) {
		return -1;
	}
	return (int)n;
}

struct HsAuth {
	char path[512];
	int missing;
	int corrupt;
	int iterations;
	uint8_t salt[16];
	uint8_t hash[32];
	int failures;
	int64_t lockout_until;
	int64_t unlocked_until;
	int64_t idle_ms;
};

static int random_bytes(uint8_t *out, size_t n) {
#ifdef _WIN32
	HCRYPTPROV prov = 0;
	if (!CryptAcquireContextA(&prov, NULL, NULL, PROV_RSA_FULL, CRYPT_VERIFYCONTEXT)) {
		return -1;
	}
	if (!CryptGenRandom(prov, (DWORD)n, out)) {
		CryptReleaseContext(prov, 0);
		return -1;
	}
	CryptReleaseContext(prov, 0);
	return 0;
#else
	int fd = open("/dev/urandom", O_RDONLY);
	size_t got = 0;
	if (fd < 0) {
		return -1;
	}
	while (got < n) {
		ssize_t r = read(fd, out + got, n - got);
		if (r <= 0) {
			close(fd);
			return -1;
		}
		got += (size_t)r;
	}
	close(fd);
	return 0;
#endif
}

static int field(const char *text, const char *key, char *out, size_t cap) {
	const char *line = text;
	size_t key_len = strlen(key);
	while (*line) {
		const char *end = strchr(line, '\n');
		size_t len = end ? (size_t)(end - line) : strlen(line);
		if (len > key_len + 1 && strncmp(line, key, key_len) == 0 && line[key_len] == '=') {
			size_t vlen = len - key_len - 1;
			if (vlen >= cap) {
				return -1;
			}
			memcpy(out, line + key_len + 1, vlen);
			out[vlen] = '\0';
			if (vlen && out[vlen - 1] == '\r') {
				out[vlen - 1] = '\0';
			}
			return 0;
		}
		if (!end) {
			break;
		}
		line = end + 1;
	}
	return -1;
}

static void load_file(HsAuth *auth) {
	FILE *fp;
	char text[2048];
	size_t n;
	char value[128];
	long iterations;
	long failures;
	long long until;
	int salt_n;
	int hash_n;
	auth->missing = 1;
	auth->corrupt = 0;
	fp = fopen(auth->path, "rb");
	if (!fp) {
		return;
	}
	n = fread(text, 1, sizeof text - 1, fp);
	fclose(fp);
	text[n] = '\0';
	auth->missing = 0;
	if (field(text, "password", value, sizeof value) == 0 || field(text, "secret", value, sizeof value) == 0) {
		auth->corrupt = 1;
		return;
	}
	if (field(text, "version", value, sizeof value) || strcmp(value, "1") != 0) {
		auth->corrupt = 1;
		return;
	}
	if (field(text, "algorithm", value, sizeof value) || strcmp(value, "PBKDF2WithHmacSHA256") != 0) {
		auth->corrupt = 1;
		return;
	}
	if (field(text, "iterations", value, sizeof value)) {
		auth->corrupt = 1;
		return;
	}
	iterations = strtol(value, NULL, 10);
	if (iterations < HS_AUTH_MIN_ITERS || iterations > HS_AUTH_MAX_ITERS) {
		auth->corrupt = 1;
		return;
	}
	failures = 0;
	until = 0;
	if (field(text, "failures", value, sizeof value) == 0) {
		failures = strtol(value, NULL, 10);
	}
	if (field(text, "lockoutUntilEpochMs", value, sizeof value) == 0) {
		until = strtoll(value, NULL, 10);
	}
	if (failures < 0 || until < 0) {
		auth->corrupt = 1;
		return;
	}
	{
		char salt_b64[128];
		char hash_b64[128];
		if (field(text, "salt", salt_b64, sizeof salt_b64) || field(text, "hash", hash_b64, sizeof hash_b64)) {
			auth->corrupt = 1;
			return;
		}
		salt_n = b64_decode(salt_b64, auth->salt, sizeof auth->salt);
		hash_n = b64_decode(hash_b64, auth->hash, sizeof auth->hash);
	}
	if (salt_n != 16 || hash_n != 32) {
		auth->corrupt = 1;
		return;
	}
	auth->iterations = (int)iterations;
	auth->failures = (int)failures;
	auth->lockout_until = until;
}

HsAuth *hs_auth_open(const char *path) {
	HsAuth *auth = calloc(1, sizeof *auth);
	if (!auth) {
		return NULL;
	}
	snprintf(auth->path, sizeof auth->path, "%s", path ? path : "");
	auth->idle_ms = 15 * 60 * 1000LL;
	load_file(auth);
	return auth;
}

void hs_auth_close(HsAuth *auth) {
	free(auth);
}

void hs_auth_set_idle_ms(HsAuth *auth, int64_t idle_ms) {
	if (auth && idle_ms > 0) {
		auth->idle_ms = idle_ms;
	}
}

int hs_auth_missing(const HsAuth *auth) {
	return auth == NULL || (auth->missing && !auth->corrupt);
}

int hs_auth_corrupt(const HsAuth *auth) {
	return auth != NULL && auth->corrupt;
}

int hs_auth_allows(HsAuth *auth, int64_t now_ms) {
	if (auth == NULL || auth->corrupt || auth->missing || auth->unlocked_until <= 0) {
		return 0;
	}
	if (now_ms >= auth->unlocked_until) {
		auth->unlocked_until = 0;
		return 0;
	}
	return 1;
}

void hs_auth_lock(HsAuth *auth, int64_t now_ms) {
	(void)now_ms;
	if (auth) {
		auth->unlocked_until = 0;
	}
}

void hs_auth_touch(HsAuth *auth, int64_t now_ms) {
	if (hs_auth_allows(auth, now_ms)) {
		auth->unlocked_until = now_ms + auth->idle_ms;
	}
}

static int write_record(HsAuth *auth) {
	char salt_b64[64];
	char hash_b64[80];
	char body[512];
	char tmp[540];
	FILE *fp;
	if (b64_encode(auth->salt, 16, salt_b64, sizeof salt_b64) < 0) {
		return -1;
	}
	if (b64_encode(auth->hash, 32, hash_b64, sizeof hash_b64) < 0) {
		return -1;
	}
	snprintf(body, sizeof body, "version=1\nalgorithm=PBKDF2WithHmacSHA256\niterations=%d\nsalt=%s\nhash=%s\nfailures=%d\nlockoutUntilEpochMs=%lld\n", auth->iterations, salt_b64, hash_b64, auth->failures, (long long)auth->lockout_until);
	snprintf(tmp, sizeof tmp, "%s.tmp", auth->path);
	fp = fopen(tmp, "wb");
	if (!fp) {
		return -1;
	}
	if (fwrite(body, 1, strlen(body), fp) != strlen(body)) {
		fclose(fp);
		remove(tmp);
		return -1;
	}
	fclose(fp);
#ifndef _WIN32
	chmod(tmp, 0600);
#endif
	if (rename(tmp, auth->path) != 0) {
		remove(tmp);
		return -1;
	}
#ifndef _WIN32
	chmod(auth->path, 0600);
#endif
	return 0;
}

int hs_auth_enroll(HsAuth *auth, const char *password, const char *confirm, int64_t now_ms, int iterations) {
	if (auth == NULL) {
		return 8;
	}
	load_file(auth);
	if (auth->corrupt) {
		return 2;
	}
	if (!auth->missing) {
		return 5;
	}
	if (password == NULL || confirm == NULL || password[0] == '\0' || confirm[0] == '\0') {
		return 1;
	}
	if ((int)strlen(password) < HS_AUTH_MIN_PASSWORD) {
		return 3;
	}
	if (strcmp(password, confirm) != 0) {
		return 4;
	}
	if (iterations < HS_AUTH_MIN_ITERS || iterations > HS_AUTH_MAX_ITERS) {
		return 8;
	}
	if (random_bytes(auth->salt, 16) != 0) {
		return 8;
	}
	if (hs_pbkdf2_sha256((const uint8_t *)password, strlen(password), auth->salt, 16, iterations, auth->hash) != 0) {
		return 8;
	}
	auth->iterations = iterations;
	auth->failures = 0;
	auth->lockout_until = 0;
	auth->missing = 0;
	if (write_record(auth) != 0) {
		auth->missing = 1;
		return 8;
	}
	auth->unlocked_until = now_ms + auth->idle_ms;
	return 0;
}

static int constant_equal(const uint8_t *a, const uint8_t *b, size_t n) {
	unsigned diff = 0;
	size_t i;
	for (i = 0; i < n; i++) {
		diff |= (unsigned)(a[i] ^ b[i]);
	}
	return diff == 0;
}

int hs_auth_verify(HsAuth *auth, const char *password, int64_t now_ms) {
	uint8_t actual[32];
	if (auth == NULL) {
		return 8;
	}
	load_file(auth);
	if (auth->corrupt) {
		return 2;
	}
	if (auth->missing) {
		return 1;
	}
	if (now_ms < auth->lockout_until) {
		return 7;
	}
	if (password == NULL || password[0] == '\0') {
		return 1;
	}
	if (hs_pbkdf2_sha256((const uint8_t *)password, strlen(password), auth->salt, 16, auth->iterations, actual) != 0) {
		return 8;
	}
	if (!constant_equal(actual, auth->hash, 32)) {
		auth->failures++;
		if (auth->failures >= 5) {
			int extra = auth->failures - 5;
			long long delay = 30000;
			int i;
			for (i = 0; i < extra && delay < 15LL * 60LL * 1000LL; i++) {
				delay *= 2;
				if (delay > 15LL * 60LL * 1000LL) {
					delay = 15LL * 60LL * 1000LL;
				}
			}
			auth->lockout_until = now_ms + delay;
		}
		write_record(auth);
		return auth->lockout_until > now_ms ? 7 : 6;
	}
	auth->failures = 0;
	auth->lockout_until = 0;
	if (write_record(auth) != 0) {
		auth->unlocked_until = 0;
		return 2;
	}
	auth->unlocked_until = now_ms + auth->idle_ms;
	return 0;
}

const char *hs_auth_status(const HsAuth *auth, int64_t now_ms) {
	if (auth == NULL || (auth->missing && !auth->corrupt)) {
		return "未配置";
	}
	if (auth->corrupt) {
		return "口令文件损坏，已锁定";
	}
	if (auth->lockout_until > now_ms) {
		return "锁定";
	}
	if (auth->unlocked_until > now_ms) {
		return "已进入";
	}
	return "已锁定";
}
