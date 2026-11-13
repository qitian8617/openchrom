cmake_minimum_required(VERSION 3.20)
project(hwstation C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

set(HWSTATION_CORE
	src/layout.c
	src/proto.c
	src/signal.c
	src/auth.c
	src/live.c
	src/session.c)

add_library(hwstation_core STATIC ${HWSTATION_CORE})
target_include_directories(hwstation_core PUBLIC src)

add_executable(hwstation-tests tests/test_main.c)
target_link_libraries(hwstation-tests PRIVATE hwstation_core)

if(MSVC)
	# /external keeps Windows SDK headers from failing /WX. /wd5105 is the
	# known C11 macro warning inside those headers on MSVC 14.5.
	set(HWSTATION_MSVC_WARNINGS /utf-8 /W4 /WX /external:W0 /external:anglebrackets /wd5105)
	target_compile_options(hwstation_core PRIVATE ${HWSTATION_MSVC_WARNINGS})
	target_compile_options(hwstation-tests PRIVATE ${HWSTATION_MSVC_WARNINGS})
	target_compile_definitions(hwstation_core PRIVATE _CRT_SECURE_NO_WARNINGS)
	target_compile_definitions(hwstation-tests PRIVATE _CRT_SECURE_NO_WARNINGS)
	target_link_libraries(hwstation-tests PRIVATE ws2_32 advapi32)
elseif(WIN32)
	target_compile_options(hwstation_core PRIVATE -Wall -Wextra -Werror)
	target_compile_options(hwstation-tests PRIVATE -Wall -Wextra -Werror)
	target_link_libraries(hwstation-tests PRIVATE ws2_32 advapi32)
else()
	target_compile_options(hwstation_core PRIVATE -Wall -Wextra -Werror)
	target_compile_options(hwstation-tests PRIVATE -Wall -Wextra -Werror)
	target_link_libraries(hwstation-tests PRIVATE m)
endif()

if(WIN32 AND CMAKE_SIZEOF_VOID_P EQUAL 8)
	add_executable(CtrlPanel WIN32 win/ctrlpanel.c)
	target_link_libraries(CtrlPanel PRIVATE hwstation_core user32 gdi32 ws2_32 advapi32)
	target_compile_definitions(CtrlPanel PRIVATE UNICODE _UNICODE)
	if(MSVC)
		target_compile_options(CtrlPanel PRIVATE ${HWSTATION_MSVC_WARNINGS})
		target_compile_definitions(CtrlPanel PRIVATE _CRT_SECURE_NO_WARNINGS)
		target_link_options(CtrlPanel PRIVATE "/ENTRY:wWinMainCRTStartup")
	else()
		target_compile_options(CtrlPanel PRIVATE -Wall -Wextra -Werror)
		target_link_options(CtrlPanel PRIVATE -municode)
	endif()
endif()

if(WIN32 AND CMAKE_SIZEOF_VOID_P EQUAL 4)
	add_executable(hwbridge win/hwbridge.c)
	add_library(fake_hwsend SHARED win/fake_hwsend.c)
	add_library(hwstation_ipc_repro SHARED win/ipc_repro.c)
	if(MSVC)
		target_compile_options(hwbridge PRIVATE ${HWSTATION_MSVC_WARNINGS})
		target_compile_options(fake_hwsend PRIVATE ${HWSTATION_MSVC_WARNINGS})
		target_compile_options(hwstation_ipc_repro PRIVATE ${HWSTATION_MSVC_WARNINGS})
		target_sources(fake_hwsend PRIVATE win/fake_hwsend.msvc.def)
		target_sources(hwstation_ipc_repro PRIVATE win/ipc_repro.msvc.def)
		target_link_libraries(hwstation_ipc_repro PRIVATE user32)
	else()
		target_compile_options(hwbridge PRIVATE -Wall -Wextra -Werror)
		target_compile_options(fake_hwsend PRIVATE -Wall -Wextra -Werror)
		target_compile_options(hwstation_ipc_repro PRIVATE -Wall -Wextra -Werror)
		target_sources(fake_hwsend PRIVATE win/fake_hwsend.def)
		target_sources(hwstation_ipc_repro PRIVATE win/ipc_repro.def)
		target_link_options(fake_hwsend PRIVATE -Wl,--kill-at -Wl,--enable-stdcall-fixup)
		target_link_options(hwstation_ipc_repro PRIVATE -Wl,--kill-at -Wl,--enable-stdcall-fixup)
		target_link_libraries(hwstation_ipc_repro PRIVATE user32)
	endif()
	set_target_properties(fake_hwsend PROPERTIES PREFIX "" OUTPUT_NAME "fake_hwsend")
	set_target_properties(hwstation_ipc_repro PROPERTIES PREFIX "" OUTPUT_NAME "hwstation_ipc_repro")
endif()
