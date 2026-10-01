#!/bin/bash

export JAVA_HOME=/d/opt64/android/studio/jbr
export ANDROID_HOME=/d/opt64/android/sdk
export PATH=$PATH:/d/opt64/scrcpy-win64-v4.1


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
DEVICE_SERIAL="SGD6IZGQGUIF9LZL"          # 为空表示使用唯一已连接设备
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
if [[ -n "$DEVICE_SERIAL" ]]; then
    ADB=(adb -s "$DEVICE_SERIAL")
else
    ADB=(adb)
fi

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
if [[ -z "$DEVICE_SERIAL" && "$device_count" -gt 1 ]]; then
    echo "错误：检测到多台设备，请用 -s 指定设备序列号：" >&2
    adb devices >&2
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
