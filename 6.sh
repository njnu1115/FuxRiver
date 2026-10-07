#!/bin/bash
set -euo pipefail

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
		export JAVA_HOME=/d/opt64/android/studio/jbr
		export ANDROID_HOME=/d/opt64/android/sdk
		export PATH=$PATH:/d/opt64/scrcpy-win64-v4.1
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


#!/usr/bin/env bash
#
# adb_tap_sequence.sh
# 功能：循环执行一组点击序列。
# 单轮流程：点击 (1066, 655) -> 等待 18 秒 -> 点击 (1296, 208) -> 等待 5 秒
#
# 用法：
#   bash adb_tap_sequence.sh                    # 执行 1 次（默认）
#   bash adb_tap_sequence.sh -n 10              # 循环执行 10 次
#   bash adb_tap_sequence.sh -s <设备序列号>    # 多设备时指定设备
#   bash adb_tap_sequence.sh -n 10 -s <序列号>  # 指定设备并循环 10 次
#   bash adb_tap_sequence.sh -h                 # 查看帮助
#
# 说明：坐标单位为像素，原点 (0,0) 位于屏幕左上角。

set -euo pipefail

# ---------- 可调参数 ----------
LOOPS=1                   # 循环次数，默认 1 次
X1=1066
Y1=655
WAIT1=18                  # 第一次点击后的等待秒数
X2=1296
Y2=208
WAIT2=5                   # 第二次点击后的等待秒数
LOOP_INTERVAL=0           # 每轮结束后到下一轮开始前的额外间隔秒数（默认 0）

# ---------- 解析可选参数 ----------
while getopts ":n:s:h" opt; do
    case "$opt" in
        n)
            if ! [[ "$OPTARG" =~ ^[0-9]+$ ]] || [[ "$OPTARG" -lt 1 ]]; then
                echo "错误：-n 必须是一个 >= 1 的正整数，实际传入: $OPTARG" >&2
                exit 1
            fi
            LOOPS="$OPTARG"
            ;;
        s) DEVICE_SERIAL="$OPTARG" ;;
        h)
            echo "用法: $0 [-n 循环次数] [-s 设备序列号]"
            echo "  -n  循环次数，正整数，默认 1"
            echo "  -s  指定 adb 设备序列号（多设备时使用）"
            echo "  -h  显示此帮助"
            exit 0
            ;;
        \?)
            echo "未知参数: -$OPTARG" >&2
            exit 1
            ;;
        :)
            echo "参数 -$OPTARG 需要取值" >&2
            exit 1
            ;;
    esac
done

# ---------- 组装 adb 基础命令 ----------
ADB=(adb)

# ---------- 检查 adb 与设备 ----------
if ! command -v adb >/dev/null 2>&1; then
    echo "错误：未找到 adb，请先安装 Android Platform Tools 并加入 PATH。" >&2
    exit 1
fi

device_count=$(adb devices | awk 'NR>1 && $2=="device" {c++} END {print c+0}')
if [[ "$device_count" -eq 0 ]]; then
    echo "错误：没有检测到已连接并授权的设备。请检查 USB 调试授权状态。" >&2
    exit 1
fi

# ---------- 点击函数 ----------
tap() {
    local x="$1" y="$2"
    echo "  -> 点击坐标 ($x, $y)"
    "${ADB[@]}" shell input tap "$x" "$y"
}

# ---------- 等待函数 ----------
wait_seconds() {
    local secs="$1"
    echo "  -> 等待 ${secs} 秒"
    sleep "$secs"
}

# ---------- 单轮序列 ----------
run_once() {
    local round="$1"
    echo ""
    echo "========== 第 ${round}/${LOOPS} 轮 =========="
    tap "$X1" "$Y1"
    wait_seconds "$WAIT1"
    tap "$X2" "$Y2"
    wait_seconds "$WAIT2"
}

# ---------- 主流程 ----------
echo "=== 开始执行点击序列，共 ${LOOPS} 轮 ==="

start_time=$(date +%s)

for ((i = 1; i <= LOOPS; i++)); do
    run_once "$i"

    # 最后一轮结束后不再额外等待
    if [[ "$i" -lt "$LOOPS" && "$LOOP_INTERVAL" -gt 0 ]]; then
        echo "  -> 轮次间隔 ${LOOP_INTERVAL} 秒"
        sleep "$LOOP_INTERVAL"
    fi
done

end_time=$(date +%s)
elapsed=$((end_time - start_time))

echo ""
echo "=== 全部 ${LOOPS} 轮执行完成，耗时 ${elapsed} 秒 ==="
