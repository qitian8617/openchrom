# 独立反控 CtrlPanel

这是放在 OpenChrom 仓库里的独立目录，不参与 Eclipse / Maven 构建，也不替换原来的温控插件。

复制到 Windows 时，放到 `E:\hwstation` 下的新子目录，例如 `E:\hwstation\publish`。已有的 `E:\hwstation\backups` 不要覆盖。

## 行为

- 启动后是仿真，主机和端口是空的
- 只有按下“连接仪器”才会连接，而且必须是手工填写的 IPv4 和端口
- “加载 DLL”才会启动旁边的 32 位 `hwbridge.exe`
- 工作站取数和仪器停热是两个按钮
- 发行包里没有厂商 `HWSendData.dll`

## 云端已编出的文件

`build/win/CtrlPanel.exe`、`build/win/hwbridge.exe`、`build/win/hwstation_ipc_repro.dll`、`build/win/fake_hwsend.dll`。

在本目录执行：

```
make test
make win
```

需要 gcc，以及 `x86_64-w64-mingw32-gcc`、`i686-w64-mingw32-gcc`。Windows 上若改用 VS 2026，请先核对 `MSBuild.exe` 的实际路径；本目录没有 VS 工程，因为所附 SDK 没有完整 `.dsp`，而界面不依赖 MFC。

`hwstation_ipc_repro.dll` 会查找 `HWClass`。不要在正在运行 HW 工作站的机器上把它当作默认 DLL 加载。测试双份是 `fake_hwsend.dll`，它不查找窗口。

## 文档

- `docs/FEATURE-MATRIX.md`
- `docs/SDK-FACTS.md`
- `docs/WINDOWS-UNVERIFIED.md`
