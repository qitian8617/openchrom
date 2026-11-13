# 从所附 SDK 源码核对到的事实

附件里有 `HWSendData.cpp`、`HWSendData.def`、`HWSendData.plg`。没有原始 `HWSendData.dll`、没有完整 `.dsp`、没有说明书原件。下面只写源码里能直接看到的内容。

`SendDataToHW` 的原文是：

- `extern "C" int WINAPI EXPORT SendDataToHW(long data1, long data2, long data3)`
- `COPYDATASTRUCT.dwData = 0xCCCC`
- 载荷是 3 个 `long`，`cbData = sizeof(long) * 3`
- `CWnd::FindWindow("HWClass", NULL)`，找到后 `SendMessage(WM_COPYDATA, ...)`
- 找到窗口并调用 `SendMessage` 后返回 1，否则返回 0
- 返回值没有第二段应答。返回 1 不是“已经开始采集”

`SendDataToHW1` … `SendDataToHW100` 在 cpp 里都有，窗口类是 `HWClass` 加编号。`.def` 导出了不带编号的 `SendDataToHW`、`SendDataToHW1`–`SendDataToHW40`、`SendDataToHW51`–`SendDataToHW100`。**41–50 在 cpp 里有函数，但不在导出表里。** 本程序拒绝按 41–50 调用。

cpp 的入口函数写成 `DLLMain`，不是 `DllMain`。`SendDataToHW` 不使用那个模块句柄。plg 只写了 Win32 Release、0 error，没有编译命令，不能据此重现原厂二进制。

cpp **没有**这些定义：`CtrlPanel.exe`、`ControlPanelClass`、互斥量名字、`data3` 含义、`HWFrequence.txt` 语法、`hwsys.dll`、`ControlPanelInfo.txt`。它们来自说明书审阅，不是这份 cpp。互斥量名字审阅里也没有，本程序用自己的 `Local\Hwstation.ControlPanelClass.instance`，不把它说成 SDK 原名。

`data3` 的 0/1/2/3/4/5 含义按说明书审阅使用，并在界面上写成“审阅结论”。`HWFrequence.txt` 不读不写。

厂商 DLL 不在本发行包里。`hwstation_ipc_repro.dll` 是按上述 cpp 行为重写的隔离桥，输出名不是 `HWSendData.dll`。它仍会查找 `HWClass`。测试没有调用它，也没有对真实工作站窗口发消息。日常加载请把路径指到你自己持有的 DLL。
