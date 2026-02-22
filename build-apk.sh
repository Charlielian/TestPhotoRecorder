#!/bin/bash

# 测试照片记录软件 APK 构建脚本
# 用于本地构建 Android APK 文件

set -e

echo "=========================================="
echo "测试照片记录软件 APK 构建脚本"
echo "=========================================="
echo ""

# 检查是否在正确的目录
if [ ! -f "gradlew" ]; then
    echo "错误: 请在 测试照片记录软件 项目根目录运行此脚本"
    exit 1
fi

# 赋予 gradlew 执行权限
echo "1. 设置 Gradle 权限..."
chmod +x gradlew

# 清理项目
echo ""
echo "2. 清理项目..."
./gradlew clean

# 构建 Debug APK
echo ""
echo "3. 构建 Debug APK..."
./gradlew assembleDebug

# 构建 Release APK
echo ""
echo "4. 构建 Release APK..."
./gradlew assembleRelease

# 显示构建结果
echo ""
echo "=========================================="
echo "构建完成!"
echo "=========================================="
echo ""
echo "APK 文件位置:"
echo "  Debug APK:   app/build/outputs/apk/debug/app-debug.apk"
echo "  Release APK: app/build/outputs/apk/release/app-release-unsigned.apk"
echo ""
echo "注意: Release APK 需要签名才能安装到设备上"
echo ""

# 显示 APK 文件信息
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    DEBUG_SIZE=$(du -h "app/build/outputs/apk/debug/app-debug.apk" | cut -f1)
    echo "Debug APK 大小: $DEBUG_SIZE"
fi

if [ -f "app/build/outputs/apk/release/app-release-unsigned.apk" ]; then
    RELEASE_SIZE=$(du -h "app/build/outputs/apk/release/app-release-unsigned.apk" | cut -f1)
    echo "Release APK 大小: $RELEASE_SIZE"
fi

echo ""
echo "=========================================="
