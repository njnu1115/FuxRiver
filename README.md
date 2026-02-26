# UI Automator 方式运行支付宝签到脚本



## 使用步骤

### 1. 前提条件
```bash
# 安装 Android Studio 和 SDK
# 确保 adb 可用
adb devices
```

### 2. 编译和运行

```bash
# 进入项目目录
cd /media/some/src/njnu1115/FuxRiver

# 编译（如果有 gradle 环境）
./gradlew assembleAndroidTest

# 或使用 Android Studio 编译生成 APK

# 安装测试 APK 到设备
adb install -r app-androidTest.apk

# 运行测试
PATH=$PATH:/Users/cl/opt/platform-tools adb -s 192.168.1.240:33333 shell am instrument -w -m -e class cn.demo.xriver.test#testAlipaySignIn cn.demo.xriver.test/androidx.test.runner.AndroidJUnitRunner

```


