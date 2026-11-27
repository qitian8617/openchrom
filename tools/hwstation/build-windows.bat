@echo off
rem Build with the installed Visual Studio. Does not modify PATH.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\build-windows.ps1"
exit /b %ERRORLEVEL%
