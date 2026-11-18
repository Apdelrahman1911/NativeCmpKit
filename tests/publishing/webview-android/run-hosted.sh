#!/usr/bin/env bash
set -euo pipefail

candidate_repo=$1
version=$2
api=$3
project="$PWD/tests/publishing/webview-android"
export ANDROID_AVD_HOME="$project/build/avd"
export ANDROID_USER_HOME="$project/build/android-user"
mkdir -p "$ANDROID_AVD_HOME" "$ANDROID_USER_HOME"

sdk_tools="$ANDROID_HOME/cmdline-tools/latest/bin"
image="system-images;android-$api;google_apis;x86_64"
"$sdk_tools/sdkmanager" 'platforms;android-37.0' "$image"
printf 'no\n' | "$sdk_tools/avdmanager" create avd --name kira-consumer --package "$image"
sudo chown "$USER" /dev/kvm
"$ANDROID_HOME/emulator/emulator" @kira-consumer -no-window -no-audio -no-boot-anim \
    -no-snapshot -gpu swiftshader_indirect > "$project/build/emulator.log" 2>&1 &
emulator_pid=$!
trap 'kill "$emulator_pid" 2>/dev/null || true' EXIT

timeout 180 adb wait-for-device
for ((attempt=0; attempt<90; attempt++)); do
    if [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = 1 ]; then break; fi
    sleep 2
done
test "$(adb shell getprop sys.boot_completed | tr -d '\r')" = 1
test "$(adb shell getprop ro.build.version.sdk | tr -d '\r')" = "$api"
adb shell input keyevent 82
adb shell dumpsys webviewupdate

./gradlew -p "$project" --no-daemon --max-workers=1 \
    '-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8' -Pkotlin.compiler.execution.strategy=in-process \
    -PnativecmpkitVersion="$version" -PnativecmpkitRepository="$candidate_repo" connectedDebugAndroidTest

python3 - "$project/build/outputs/androidTest-results" <<'PY'
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

tests = [case for path in Path(sys.argv[1]).rglob("TEST-*.xml")
         for case in ET.parse(path).getroot().iter("testcase")]
assert len(tests) == 1 and tests[0].get("name") == "loadsOwnedHtmlAndDisposesPublishedWebView", tests
assert all(child.tag not in {"skipped", "failure", "error"} for child in tests[0]), ET.tostring(tests[0], encoding="unicode")
print("Verified one passed external Android WebView method.")
PY
