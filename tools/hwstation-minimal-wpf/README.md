# 最小工作站接入（CtrlPanel）

只做一件事：连上当前色谱仪的 PC 采集口，把 `ACQ_DATA` 里的对数电压毫伏换算成 fA，在操作者明确放行后送给已安装的 HW 工作站。不改固件，不改 OpenChrom，不改 `tools/hwstation`，不改 `HWFrequence.txt`。

界面上始终有这句话：**无法验证设备样本新鲜度，仅接入验证，不用于正式定量**。设备在读失败时会重复上一个缓存值，帧里没有新样本序号。相同数值会照样送出，不能据此判断过期。

## 数据

- 握手只发 `GCWS<TAB>HELLO<TAB>PC`。收到 `HELLO_OK` 才算拿到 PC 控制权。
- 开始和停止是二进制帧，不是文本 `GCWS START_ACQ`。文本那条是面板通知，不会启动 ADC。
- `START_ACQ` 载荷是小端 100 ms。`Y(mV) = vlog_uv / 1000`，可以为负，没有 +1.25 V 偏移。
- `V = Y / 1000`，`I(pA) = 100000 * 10^(V / 0.2)`，送给工作站的是 `round(I * 1000)` fA。
- 非有限值或超出 int32 就停止转发，不截断，也不改用板上的 pA 饱和值。
- 时间不是每 100 ms、帧序号或批序号断开，都判本次无效。不补点，不压缩，不为 20 Hz 复制样本。
- A 通道样本是 `(fA, 0, 3)`。工作站开始 `(1,0,1)`，停止 `(1,0,2)`。归零只有显式的 `(1,0,5)`。没有 B 通道实测，所以不发 `data3=4`，也不用 `data3=0` 把 B 写成 0。
- DLL 返回 1 只表示找到 `HWClass` 并发出了消息，不是采集成功。返回 0 表示没找到窗口。

`examples/HWFrequence.proposed-10-10.txt` 是给操作者以后自己部署的 **10,10** 候选。现在的安装值 20,20 是 50 ms，和这次 100 ms 请求不一致，转发保持关闭。10,20 也会被看成 100 ms，但程序不会写任何安装目录。

## 用法

1. 在 Windows 上先启动 HW 工作站。本程序不复制 DLL，也不打开 `D:\HWGMP`。
2. 运行 x86 的 `CtrlPanel.exe`。主机、端口、DLL 路径都是空的。
3. 手填设备地址后点连接。可以先看样本，不转发。
4. 选择本机的 `HWFrequence.txt`（只读）。勾选已知晓限制。填写厂商 DLL 的完整路径，再点“允许转发”。
5. 需要工作站记录时，再点 A 通道开始。停止设备采集不是急停，也不会关温控。

顶层窗口是手写的 `ControlPanelClass`，标题是“色谱反控（最小接入）”。WPF 只画在这个窗口里面，不会自动变成这个类名。互斥量是本程序自己的 `Local\HwstationMinimalWpf.ControlPanelClass.instance`，说明书里没有这个名字。若旧的反控程序也注册了 `ControlPanelClass`，第二次启动只激活带这个标题的窗口。

厂商 DLL 不在源码包里。加载发生在点“允许转发”之后，并且只加载文本框里的路径。

## 这一次 Windows 命令

在 `tools\hwstation-minimal-wpf` 下执行一次：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\windows-one-pass.ps1
```

脚本先跑离线测试，再编译 x86 `CtrlPanel`。机器上若有 SDK 10.0.201，只在这个进程里钉住它，不改用户环境，也不调用 `install_windows.ps1`。成功后的程序在：

`src\CtrlPanel\bin\Release\net8.0-windows\win-x86\CtrlPanel.exe`

不要加参数去连仪器。离线测试不打开套接字，不加载厂商 DLL。

Linux 上只跑核心测试：

```sh
sh scripts/test-offline.sh
```

## 还要上电才能确认的事

源码里的新平均值大约 100 ms，不是已经在这块板上测到的 DRDY 间隔。工作站是否按 10 Hz 画图，取决于操作者以后有没有自己改频率文件。DLL 返回 1 之后，谱图有没有在记，也只能看着工作站窗口确认。
