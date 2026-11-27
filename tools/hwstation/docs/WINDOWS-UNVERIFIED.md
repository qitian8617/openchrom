# Windows 上已经检查的和仍然没有做的

Windows CI（`hwstation` 工作流的 `msvc` 作业）用已安装的 Visual Studio 编译 x64 `CtrlPanel.exe`、x64 `hwstation-tests.exe` 和 x86 `hwbridge.exe`，然后执行：

- `hwstation-tests.exe --offline`
- `CtrlPanel.exe --offline-smoke`

`--offline` 只跑本地用例和一次 `127.0.0.1` 回环 HELLO。不查找 `HWClass`，不加载厂商 DLL，不连接仪器。`--offline-smoke` 建立 `ControlPanelClass` 窗口，按页核对按钮是否完整落在客户区内，然后退出。它不加载 DLL，不发送 `WM_COPYDATA`，不打开套接字。失败时日志会写出页号、页名和控件名。

Linux gcc/clang 的通过不能代替这次 Windows 作业。数量差在于：Windows 多一个 `accept thread`；`bridge i386`、`panel x64`、`repro dll i386` 只有在测试能看见这三个 PE 时才跑。

本机 v3 交叉包曾失败的 7 项（`verify`、`inside idle`、`pid unlock`、`confirmed pid`、`oven pid sent`、`inlet pid sent`、`sent edited kp`）来自 Win32 `rename` 不能覆盖已有口令文件，校验写回失败后没有解锁。用例还在，没有改成跳过。

仍然没有做：

- 操作者自己看界面是否顺眼（烟测只查控件在不在客户区内）
- 加载厂商 `HWSendData.dll`，或把 `hwstation_ipc_repro.dll` 当作默认 DLL
- 查找或操作名为 `HWClass` 的窗口
- 连接色谱仪、点火、加热、真实采集
- 核对 HW 谱图时间轴、面积和单位
- 确认 HW-2000 与 HW-2000GMP 是否同一套窗口类
- 气路示意图，以及校准表的第二路真实温度列

本机重编执行 `build-windows.bat`。不要安装 MinGW，不要改全局 PATH，不要改 OpenChrom 主线或 `D:\HWGMP`。
