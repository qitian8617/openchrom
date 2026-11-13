ifeq ($(OS),Windows_NT)
$(error Windows build uses build-windows.bat and the installed Visual Studio. This Makefile is the Linux gcc path.)
endif
CC ?= gcc
CFLAGS ?= -std=c11 -Wall -Wextra -Werror -O2
CPPFLAGS += -Isrc
LDFLAGS += -lm

SRC := src/layout.c src/proto.c src/signal.c src/auth.c src/live.c src/session.c
WIN_SRC := $(SRC)

.PHONY: all test win clean

all: test win

build/hwstation-tests: $(SRC) tests/test_main.c src/hs.h src/live.h
	mkdir -p build
	$(CC) $(CFLAGS) $(CPPFLAGS) -o $@ tests/test_main.c $(SRC) $(LDFLAGS)

test: build/hwstation-tests
	./build/hwstation-tests

build/win/CtrlPanel.exe: $(WIN_SRC) win/ctrlpanel.c src/hs.h
	mkdir -p build/win
	x86_64-w64-mingw32-gcc -std=c11 -Wall -Wextra -O2 -Isrc -DUNICODE -D_UNICODE -municode -mwindows -o $@ win/ctrlpanel.c $(WIN_SRC) -lws2_32 -ladvapi32

build/win/hwbridge.exe: win/hwbridge.c
	mkdir -p build/win
	i686-w64-mingw32-gcc -std=c11 -Wall -Wextra -O2 -o $@ win/hwbridge.c

build/win/hwstation_ipc_repro.dll: win/ipc_repro.c win/ipc_repro.def
	mkdir -p build/win
	i686-w64-mingw32-gcc -shared -O2 -o $@ win/ipc_repro.c win/ipc_repro.def -Wl,--kill-at -Wl,--enable-stdcall-fixup

build/win/fake_hwsend.dll: win/fake_hwsend.c win/fake_hwsend.def
	mkdir -p build/win
	i686-w64-mingw32-gcc -shared -O2 -o $@ win/fake_hwsend.c win/fake_hwsend.def -Wl,--kill-at -Wl,--enable-stdcall-fixup

win: build/win/CtrlPanel.exe build/win/hwbridge.exe build/win/hwstation_ipc_repro.dll build/win/fake_hwsend.dll

clean:
	rm -rf build
