# 尚未在 Windows 实机上做的事

云端完成：

- `tools/hwstation` 的 C 核心用本机 gcc 编译，`./build/hwstation-tests` 输出 `ok`
- 测试覆盖命令分类、0.1 MPa 门槛、pA→fA 溢出、10 Hz 不按 50 ms 推送、丢点中止、离线不写套接字、127.0.0.1 假仪器只收到 HELLO、维护口令与 Java PBKDF2 向量、日志上限、单实例锁、按钮换行
- `x86_64-w64-mingw32-gcc` 生成 `CtrlPanel.exe`（PE32+，窗口类源码字符串是 `ControlPanelClass`）
- `i686-w64-mingw32-gcc` 生成 `hwbridge.exe` 和 `hwstation_ipc_repro.dll`（PE32 / i386）
- 导出表有 91 个 `SendDataToHW*`，没有 41–50

没有做：

- 在 Windows 上双击或用 VS 启动 `CtrlPanel.exe`
- 用 Wine 或真机加载厂商 `HWSendData.dll`
- 查找或操作名为 `HWClass` 的窗口
- 连接色谱仪、点火、加热、采集
- 核对 HW 谱图时间轴、面积和单位
- 确认 HW-2000 与 HW-2000GMP 是否同一套窗口类

本机后续如果要编，VS 路径要自己核对。这次没有改系统 Java、没有改 Maven、没有改 `D:\HWGMP`。
