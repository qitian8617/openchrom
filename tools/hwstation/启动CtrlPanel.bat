@echo off
cd /d "%~dp0"
echo 本脚本只启动本目录的 CtrlPanel.exe。
echo 不要覆盖 E:\hwstation\backups。
echo 默认是仿真，不会连接仪器，也不会加载 DLL。
if not exist "%~dp0CtrlPanel.exe" (
  echo 找不到 CtrlPanel.exe。请先按 README 构建，或使用已打包的 Windows 发行目录。
  exit /b 1
)
start "" "%~dp0CtrlPanel.exe"
