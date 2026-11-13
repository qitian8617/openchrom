# 独立反控 CtrlPanel

这是放在 OpenChrom 仓库里的独立目录，不参与 Eclipse / Maven 构建，也不替换原来的温控插件。

复制到 Windows 时，放到 `E:\hwstation` 下的新子目录，例如 `E:\hwstation\publish`。已有的 `E:\hwstation\backups` 不要覆盖。

## 行为

- 启动后是仿真，主机和端口是空的
- 只有按下“连接仪器”才会连接，而且必须是手工填写的 IPv4 和端口
- “加载 DLL”才会启动旁边的 32 位 `hwbridge.exe`
- 工作站取数和仪器停热是两个按钮
- 发行包里没有厂商 `HWSendData.dll`

## Windows 构建（已安装的 Visual Studio，不需要 MinGW）

在本目录执行 `build-windows.bat`。脚本只用本机的 `vswhere` 和 CMake，不改全局 PATH。它会编出：

- x64 `CtrlPanel.exe`
- x64 `hwstation-tests.exe`
- x86 `hwbridge.exe`、`fake_hwsend.dll`、`hwstation_ipc_repro.dll`

产物在 `dist-msvc\`。不连仪器的检查：

```
dist-msvc\hwstation-tests.exe --offline
dist-msvc\CtrlPanel.exe --offline-smoke
```

`--offline` 只跑本地用例和 `127.0.0.1` 回环握手，不查找 `HWClass`。`--offline-smoke` 建立窗口、核对分页按钮后退出，不加载 DLL，不发 `WM_COPYDATA`，不连接网络。

## Linux 上的同一套测试

```
cmake -S . -B build -DCMAKE_BUILD_TYPE=Release
cmake --build build
./build/hwstation-tests --offline
```

`make test` 仍可用，但 Windows 机器上的正式入口是上面的 CMake/MSVC 脚本。

`hwstation_ipc_repro.dll` 会查找 `HWClass`。不要在正在运行 HW 工作站的机器上把它当作默认 DLL 加载。测试双份是 `fake_hwsend.dll`，它不查找窗口。

## 文档

- `docs/FEATURE-MATRIX.md`
- `docs/SDK-FACTS.md`
- `docs/WINDOWS-UNVERIFIED.md`
