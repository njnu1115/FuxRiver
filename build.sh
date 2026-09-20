#!/bin/bash

export JAVA_HOME='/home/linuxbrew/.linuxbrew/Cellar/openjdk@17/17.0.20.1'
export ANDROID_HOME=/mnt/nas2/CharLIU/opt/android/sdk
export PATH=$PATH:/mnt/nas2/CharLIU/opt/android/sdk/platform-tools

# export ANDROID_SERIAL=SGD6IZGQGUIF9LZL
# export ANDROID_SERIAL=662846a7
export ANDROID_SERIAL=localhost:33333


#!/bin/bash

# 全局变量，用于存储后台 logcat 的进程 ID
LOGCAT_PID=""

cleanup() {
    echo ""
    echo ">>> 捕获到中断信号，正在执行清理操作..."
    
    # 1. 停止后台的 logcat 进程
    if [ -n "$LOGCAT_PID" ]; then
        kill $LOGCAT_PID 2>/dev/null
        wait $LOGCAT_PID 2>/dev/null # 等待进程彻底结束，防止僵尸进程
        echo ">>> 已停止后台 logcat (PID: $LOGCAT_PID)"
    fi
    
    # 2. 停止目标应用
    adb shell am force-stop cn.demo.xriver.test
    
    echo ">>> 清理完成，准备退出脚本。"
    exit 0 
}

# 捕获 SIGINT (Ctrl+C) 和 SIGTERM 信号
trap cleanup SIGINT SIGTERM

echo "========================================="
echo "脚本已启动，正在运行中... (按 Ctrl+C 停止)"
echo "目标应用: cn.demo.xriver.test"
echo "========================================="

# 编译和安装
./gradlew assembleAndroidTest
adb install -r ./build/outputs/apk/androidTest/debug/FuxRiver-debug-androidTest.apk

echo ""
echo ">>> 清空手机旧日志缓存..."
adb logcat -c 

echo ">>> 启动后台 logcat 实时监听..."
# 核心：在后台运行 logcat，& 表示放入后台。
# 注意：这里的 FuxRiver19890604 需要和你 Java 代码里的 Log.d(TAG, msg) 的 TAG 保持一致！
adb logcat -s FuxRiver19890604:D &
LOGCAT_PID=$! # $! 表示上一个后台进程的 PID

echo ">>> 开始运行 UIAutomator 测试..."
# 运行测试 (阻塞执行，期间终端只会显示 logcat 的输出)
adb shell am instrument -w -m -e class cn.demo.xriver.AlipaySignInTest#testAlipaySignIn cn.demo.xriver.test/androidx.test.runner.AndroidJUnitRunner

# 测试正常结束后，主动清理后台 logcat
if [ -n "$LOGCAT_PID" ]; then
    kill $LOGCAT_PID 2>/dev/null
    wait $LOGCAT_PID 2>/dev/null
    echo ""
    echo ">>> 测试运行结束，已关闭后台 logcat。"
fi
