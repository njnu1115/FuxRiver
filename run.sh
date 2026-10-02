#!/bin/bash
# export ANDROID_SERIAL=SGD6IZGQGUIF9LZL
# export ANDROID_SERIAL=662846a7
set -e

OS_TYPE=$(uname -s)

case "$OS_TYPE" in
    Linux*)
        echo "当前是 Linux"
        export JAVA_HOME='/home/linuxbrew/.linuxbrew/Cellar/openjdk@17/17.0.20.1'
        export ANDROID_HOME=/mnt/nas2/CharLIU/opt/android/sdk
        export PATH=$PATH:/mnt/nas2/CharLIU/opt/android/sdk/platform-tools
        ;;
    Darwin*)
        echo "当前是 macOS"
        export JAVA_HOME=/Users/cl/homebrew/Cellar/openjdk@17/17.0.20.1
        export ANDROID_HOME=/Users/cl/Library/Android/sdk
        export PATH=$PATH:/Users/cl/opt/scrcpy-macos-aarch64-v4.0
        ;;
    MINGW*|MSYS*|CYGWIN*)
        echo "当前是 Windows (Git Bash / MinGW / MSYS / Cygwin)"
        ;;
    *)
        echo "未知操作系统: $OS_TYPE"
        ;;
esac

# 获取可用设备列表（过滤掉表头和空行，只保留状态为 "device" 的设备）
get_available_devices() {
    adb devices | awk 'NR>1 && $2=="device" {print $1}'
}

# 主逻辑
select_device() {
    # 1. 优先使用环境变量
    if [ -n "$ANDROID_SERIAL" ]; then
        echo "✓ 使用环境变量 ANDROID_SERIAL: $ANDROID_SERIAL"
        return 0
    fi

    # 2. 获取设备列表
    local devices
    devices=$(get_available_devices)
    
    # 3. 无设备则报错退出
    if [ -z "$devices" ]; then
        echo "❌ 错误：未检测到任何已连接的设备"
        echo "请检查："
        echo "  1. USB 是否连接"
        echo "  2. 是否开启了 USB 调试"
        echo "  3. 是否已在手机上授权此电脑"
        exit 1
    fi

    # 4. 统计设备数量
    local device_count
    device_count=$(echo "$devices" | wc -l | tr -d ' ')

    # 5. 单设备自动选择
    if [ "$device_count" -eq 1 ]; then
        ANDROID_SERIAL="$devices"
        export ANDROID_SERIAL
        echo "✓ 自动选择唯一设备: $ANDROID_SERIAL"
        return 0
    fi

    # 6. 多设备交互式选择
    echo "检测到多个设备，请选择一个："
    echo "--------------------------------"
    
    local i=1
    local device_array=()
    while IFS= read -r device; do
        device_array+=("$device")
        echo "  [$i] $device"
        ((i++))
    done <<< "$devices"
    
    echo "--------------------------------"
    
    # 读取用户输入
    local choice
    read -p "请输入序号 (1-$device_count): " choice
    
    # 验证输入
    if ! [[ "$choice" =~ ^[0-9]+$ ]] || [ "$choice" -lt 1 ] || [ "$choice" -gt "$device_count" ]; then
        echo "❌ 错误：无效的序号"
        exit 1
    fi

    # 设置选中的设备
    ANDROID_SERIAL="${device_array[$((choice-1))]}"
    export ANDROID_SERIAL
    echo "✓ 已选择设备: $ANDROID_SERIAL"
}

# 调用函数选择设备
select_device


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
./gradlew assembleAndroidTest || exit 1
adb install -r ./build/outputs/apk/androidTest/debug/FuxRiver-debug-androidTest.apk || exit 1

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

# 收尾清理
PKG_LIST="com.eg.android.AlipayGphone com.taobao.taobao com.baidu.searchbox \
com.baidu.searchbox.lite com.taobao.etao com.sankuai.meituan com.kuaishou.nebula"
for p in $PKG_LIST; do adb shell am force-stop "$p"; done
