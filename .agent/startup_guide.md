# RoomieSplit 前后端启动指南

## ✅ 后端状态：已启动

**服务地址**: http://localhost:8080  
**启动命令**: `./gradlew :backend:bootRun`  
**进程ID**: 60528  
**启动时间**: 3.246秒

### 后端API测试
你可以在浏览器中访问：
- http://localhost:8080 - 后端首页
- http://localhost:8080/api/v1/users/1/profile - 测试用户API

---

## 📱 前端启动方法

由于Android应用需要在模拟器或真实设备上运行，有以下几种启动方式：

### 方法1: 使用Android Studio（推荐）

1. **打开项目**
   - 启动Android Studio
   - 打开项目路径: `E:\data\roomiesplit\RoomieSplit`

2. **同步Gradle** ⚠️ 重要
   - 点击顶部的 "Sync Project with Gradle Files" 按钮（🐘图标）
   - 等待同步完成（这将下载Glide和其他依赖）

3. **启动模拟器**
   - 点击顶部工具栏的 AVD Manager（📱图标）
   - 选择一个模拟器点击 ▶️ 启动
   - 或者连接真实Android设备（需开启USB调试）

4. **运行应用**
   - 选择设备后，点击绿色的 ▶️ Run 按钮
   - 或按快捷键 `Shift + F10`

---

### 方法2: 使用命令行

#### 前提条件
需要配置Android SDK环境变量，确保以下命令可用：
- `adb` (Android Debug Bridge)
- `emulator` (Android Emulator)

#### 步骤
```powershell
# 1. 编译APK（会自动下载Glide依赖）
cd E:\data\roomiesplit\RoomieSplit
./gradlew :app:assembleDebug

# 2. 查看可用的模拟器
emulator -list-avds

# 3. 启动模拟器（替换<AVD_NAME>为实际名称）
emulator -avd <AVD_NAME>

# 4. 等待模拟器启动后，安装APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 5. 启动应用
adb shell am start -n com.example.roomiesplit/.MainActivity
```

---

## 🔧 构建问题解决

如果之前的构建失败，可能是因为：

### 1. Gradle同步问题
**解决方案**: 在Android Studio中执行以下操作：
```
File -> Invalidate Caches / Restart -> Invalidate and Restart
```

### 2. Glide依赖未下载
**解决方案**: 
```powershell
./gradlew :app:build --refresh-dependencies
```

### 3. Kotlin插件问题
检查 `app/build.gradle` 中的 Kotlin 版本是否与项目兼容

---

## 🌐 网络配置

### ⚠️ 重要：Android模拟器访问本地后端

Android模拟器不能使用 `localhost` 或 `127.0.0.1` 访问主机的服务！

需要使用特殊IP地址：
- **Android模拟器**: `10.0.2.2` 代表主机的 localhost
- **真实设备**: 使用主机的局域网IP（如 `192.168.x.x`）

### 修改后端地址配置

检查 `RetrofitClient.java` 中的 BASE_URL：

```java
// 当前配置
private static final String BASE_URL = "http://localhost:8080/";

// 应改为（模拟器）
private static final String BASE_URL = "http://10.0.2.2:8080/";

// 或（真实设备，替换为你的IP）
private static final String BASE_URL = "http://192.168.1.100:8080/";
```

---

## 📋 启动检查清单

### 后端
- [x] 后端服务已启动（端口8080）
- [x] 可以访问 http://localhost:8080

### 前端
- [ ] Android Studio已打开项目
- [ ] Gradle同步成功（无红色错误）
- [ ] 模拟器或设备已连接
- [ ] RetrofitClient.BASE_URL指向正确地址
- [ ] 应用已安装并运行

---

## 🐛 常见问题

### Q1: "Could not find com.github.bumptech.glide:glide"
**A**: Gradle未同步，在Android Studio中点击 "Sync Now"

### Q2: 应用连接不到后端
**A**: 检查BASE_URL是否使用了 `10.0.2.2`（模拟器）或正确的局域网IP（真实设备）

### Q3: "ProfileFragment.java is not on the classpath"
**A**: 这是lint警告，Gradle同步后会自动消失，不影响运行

---

## 📞 测试连接

### 1. 测试后端
浏览器访问: http://localhost:8080/api/v1/users/1/profile

### 2. 测试前端到后端连接
在应用中尝试登录，查看Logcat输出是否有网络请求

---

## 🚀 快速启动（推荐流程）

1. ✅ **后端已运行** - 保持当前终端
2. 🔵 **打开Android Studio**
3. 🔄 **Sync Gradle** - 等待完成
4. 🔧 **修改BASE_URL** - 改为 `http://10.0.2.2:8080/`
5. 📱 **启动模拟器**
6. ▶️ **运行应用**
7. 🎉 **开始测试**

---

## 📝 运行日志

**后端启动时间**: 2026-01-13 22:52
**后端端口**: 8080
**后端状态**: ✅ RUNNING

---

需要帮助？检查：
1. 后端日志（当前终端）
2. Android Studio的 Logcat
3. Gradle Build输出
