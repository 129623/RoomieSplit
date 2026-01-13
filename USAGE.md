# RoomieSplit 项目运行指南

本文档介绍如何在 Windows 环境下运行 Android 模拟器并部署 RoomieSplit 应用程序。

## 前置条件

确保已安装配置好以下环境（脚本会自动设置部分环境变量）：
- Java JDK 21 (`D:\java\jdk21`)
- Android SDK (`D:\Android\Sdk`)
- PowerShell

## 快速启动（推荐）

我们提供了一个可以自动等待模拟器就绪并运行应用的一键脚本。

1.  **启动模拟器**（如果尚未启动）：
    在 PowerShell 中运行：
    ```powershell
    .\run_emulator.ps1
    ```
    *注意：模拟器启动可能需要一些时间，请保持该窗口打开。*

2.  **构建并运行应用**：
    在新的 PowerShell 窗口中运行：
    ```powershell
    .\wait_and_run.ps1
    ```
    该脚本会：
    *   检查是否连接了设备（模拟器或真机）。
    *   如果设备未就绪，它会等待。
    *   设备就绪后，它会自动构建 Debug 版本的 APK。
    *   安装 APK 到设备。
    *   自动启动应用的主界面 (`MainActivity`)。

## 分步操作

如果您希望分步控制，可以使用以下单独的脚本：

### 1. 设置环境（可选）
如果遇到环境变量问题，可以单独运行：
```powershell
.\setup_env.ps1
```

### 2. 启动模拟器
启动名为 `Medium_Phone_API_36.1` 的模拟器：
```powershell
.\run_emulator.ps1
```

### 3. 构建应用
仅构建 APK，不安装运行：
```powershell
.\gradlew.bat assembleDebug
```

### 4. 安装并运行
构建并安装 APK，然后尝试启动应用：
```powershell
.\run_app.ps1
```

## 常见问题

*   **构建失败 "Resource not found"**：通常是因为缺少资源文件。我们已在 `app/src/main/res/mipmap` 下修复了 `ic_launcher` 问题。
*   **模拟器未找到**：请确保先运行 `run_emulator.ps1`，等待模拟器完全启动进入桌面后再运行安装脚本。
*   **端口被占用**：如果 `adb` 报错，尝试运行 `adb kill-server` 然后 `adb start-server`。
