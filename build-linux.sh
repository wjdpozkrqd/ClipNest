#!/usr/bin/env bash
set -euo pipefail
if ! command -v gradle >/dev/null 2>&1; then
  echo "未检测到系统 Gradle。请安装 Gradle 8.9，或先生成 Gradle Wrapper。"
  exit 1
fi
gradle wrapper --gradle-version 8.9
chmod +x ./gradlew
./gradlew clean assembleDebug
echo "APK: app/build/outputs/apk/debug/app-debug.apk"
