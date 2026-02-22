# 测试照片记录软件 - 构建和部署指南

## 项目概述

测试照片记录软件 是一个 Android 信号测试应用，用于监测和记录 2G/3G/4G/5G 网络信号强度，并支持拍照记录功能。

## 最新更新 (2026-02-21)

### 修复的关键Bug
1. ✅ 修复内存泄漏 - HistoryAdapter 图片加载优化
2. ✅ 替换废弃API - Cursor.getColumnIndex() 更新
3. ✅ 实现真实信号强度采集 - 移除硬编码值
4. ✅ 添加权限检查 - LocationService 安全性提升
5. ✅ 修复线程安全问题 - BatchTestActivity 同步列表
6. ✅ 迁移到应用专属存储 - 符合 Android 隐私政策
7. ✅ 添加空值安全检查 - 防止崩溃
8. ✅ 更新 SDK 到 Android 14 (API 34)

### 构建修复
- 修复布局 XML 中的 'auto' margin 问题
- 移除不兼容的 getSinr() 调用
- 注释掉布局中不存在的 5G 视图
- 使用 Android 系统图标作为占位符

## 构建 APK

### 方法 1: 使用本地脚本

```bash
cd 测试照片记录软件
./build-apk.sh
```

### 方法 2: 使用 Gradle 命令

```bash
cd 测试照片记录软件

# 清理项目
./gradlew clean

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease
```

### APK 输出位置

- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk` (7.1MB)
- **Release APK**: `app/build/outputs/apk/release/app-release-unsigned.apk`

## GitHub Actions 自动构建

项目已配置 GitHub Actions 工作流 (`build-java2.yaml`)，可自动构建 APK：

### 触发条件
- 推送到 `main` 或 `develop` 分支
- 创建 Pull Request 到 `main` 分支
- 手动触发 (workflow_dispatch)

### 构建产物
- Debug APK (带日志)
- Release APK (未签名)
- 自动创建 GitHub Release

## 推送到 GitHub

由于需要 GitHub 凭据，请手动推送：

```bash
# 方法 1: 使用 HTTPS (需要 Personal Access Token)
git push https://github.com/Charlielian/测试照片记录软件.git main

# 方法 2: 配置 SSH 密钥后使用 SSH
git remote set-url origin git@github.com:Charlielian/测试照片记录软件.git
git push origin main
```

## 安装 APK

### 在 Android 设备上安装

1. 将 APK 文件传输到设备
2. 在设备上启用"未知来源"安装
3. 点击 APK 文件进行安装

### 使用 ADB 安装

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 签名 Release APK

Release APK 需要签名才能安装：

```bash
# 创建密钥库（首次）
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias

# 签名 APK
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 -keystore my-release-key.jks app/build/outputs/apk/release/app-release-unsigned.apk my-key-alias

# 对齐 APK
zipalign -v 4 app/build/outputs/apk/release/app-release-unsigned.apk 测试照片记录软件-release.apk
```

## 系统要求

### 开发环境
- JDK 17
- Android SDK 34
- Gradle 9.2.1
- Android Gradle Plugin 9.0.1

### 运行环境
- Android 6.0 (API 23) 及以上
- 推荐 Android 12 (API 31) 及以上以获得最佳体验

## 权限要求

应用需要以下权限：
- `ACCESS_FINE_LOCATION` - 获取精确位置
- `ACCESS_COARSE_LOCATION` - 获取粗略位置
- `CAMERA` - 拍照功能
- `READ_PHONE_STATE` - 读取信号信息

## 功能特性

1. **实时信号监测** - 显示 2G/3G/4G/5G 网络信号强度
2. **双卡支持** - 支持双 SIM 卡信号监测
3. **拍照记录** - 拍照并叠加信号信息
4. **历史记录** - 查看历史信号数据
5. **图表分析** - 信号趋势可视化
6. **地图显示** - 在地图上显示测试位置
7. **批量测试** - 自动化批量信号测试
8. **数据导出** - 导出 CSV 格式报告

## 已知问题

1. Google Maps API 密钥需要配置（在 AndroidManifest.xml 中）
2. 5G 频段映射可能需要根据实际运营商调整
3. 部分 5G 相关视图在当前布局中未实现

## 技术栈

- **语言**: Java
- **最低 SDK**: API 23 (Android 6.0)
- **目标 SDK**: API 34 (Android 14)
- **主要库**:
  - AndroidX AppCompat 1.6.1
  - Material Components 1.11.0
  - CameraX 1.3.1
  - Google Play Services (Location & Maps)
  - MPAndroidChart 3.1.0

## 贡献

欢迎提交 Issue 和 Pull Request！

## 许可证

[请添加许可证信息]

## 联系方式

- GitHub: https://github.com/Charlielian/测试照片记录软件
- Issues: https://github.com/Charlielian/测试照片记录软件/issues
