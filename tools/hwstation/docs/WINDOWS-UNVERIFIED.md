# 尚未在 Windows 实机上做的事

云端完成：

- `tools/hwstation` 的 C 核心用本机 gcc 编译，`./build/hwstation-tests` 输出 `ok`。其中包含温程校验、adc 气压不能开门控、10 次稳温后才启动柱箱、停止要有 WRITE_OK 和 heat=0、PID 确认串不一致则零字节写出
- 测试覆盖命令分类、0.1 MPa 门槛、pA→fA 溢出、10 Hz 不按 50 ms 推送、丢点中止、离线不写套接字、127.0.0.1 假仪器只收到 HELLO、维护口令与 Java PBKDF2 向量、日志上限、单实例锁、按钮换行
- `x86_64-w64-mingw32-gcc` 生成 `CtrlPanel.exe`（PE32+，窗口类源码字符串是 `ControlPanelClass`）
- `i686-w64-mingw32-gcc` 生成 `hwbridge.exe` 和 `hwstation_ipc_repro.dll`（PE32 / i386）
- 导出表有 91 个 `SendDataToHW*`，没有 41–50

发行包里的 exe 是 mingw-w64 交叉编译、链接系统 msvcrt，用来在装好 Visual Studio 之前就能跑离线检查。本机正式重编请执行 `build-windows.bat`，不要安装 MinGW，也不要改全局 PATH。

请在 Windows 上执行，云端没有跑过这两个程序：

- `hwstation-tests.exe --offline`
- `CtrlPanel.exe --offline-smoke`

`--offline-smoke` 会建立窗口并核对分页后退出。它不加载 DLL，不发送 `WM_COPYDATA`，不查找 `HWClass`，也不连接网络。云端没有 Visual Studio，也没有 Wine，所以不能把这次交叉编译说成 Windows 界面已经测过。

没有做：

- 在这台云端机器上用 Visual Studio 启动界面（这里没有 VS）
- 用 Wine 或真机加载厂商 `HWSendData.dll`
- 查找或操作名为 `HWClass` 的窗口
- 连接色谱仪、点火、加热、采集
- 核对 HW 谱图时间轴、面积和单位
- 确认 HW-2000 与 HW-2000GMP 是否同一套窗口类

本机后续如果要编，VS 路径要自己核对。这次没有改系统 Java、没有改 Maven、没有改 `D:\HWGMP`。
