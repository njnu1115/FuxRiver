#!/bin/bash

export JAVA_HOME='/home/linuxbrew/.linuxbrew/Cellar/openjdk@17/17.0.20.1'
export ANDROID_HOME=/mnt/nas2/CharLIU/opt/android/sdk
export PATH=$PATH:/mnt/nas2/CharLIU/opt/android/sdk/platform-tools

# export ANDROID_SERIAL=SGD6IZGQGUIF9LZL
# export ANDROID_SERIAL=662846a7
export ANDROID_SERIAL=localhost:33333


cleanup() {
    echo ""
    echo ">>> 捕获到 Ctrl+C 信号，正在执行清理操作..."
    
    # 执行你需要的 adb 命令
    adb shell am force-stop cn.demo.xriver.test
    
    echo ">>> 清理完成，准备退出脚本。"
    
    # 2. 退出脚本 (非常重要，否则脚本会继续往下执行)
    exit 0 
}

# 使用 trap 捕获 SIGINT (Ctrl+C) 信号，并绑定到 cleanup 函数
trap cleanup SIGINT

echo "脚本已启动，正在运行中... (按 Ctrl+C 停止)"
echo "目标应用: cn.demo.xriver.test"

./gradlew assembleAndroidTest
adb install -r ./build/outputs/apk/androidTest/debug/FuxRiver-debug-androidTest.apk
adb shell am instrument -w -m -e class cn.demo.xriver.AlipaySignInTest#testAlipaySignIn cn.demo.xriver.test/androidx.test.runner.AndroidJUnitRunner
