#!/usr/bin/env bash
set -euo pipefail

candidate_repo=$1
version=$2
project="$PWD/tests/publishing/webview-ios"
./gradlew -p "$project" --no-daemon --max-workers=1 \
    '-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8' -Pkotlin.compiler.execution.strategy=in-process \
    '-Pkotlin.native.jvmArgs=-Xmx3072m' \
    -PnativecmpkitVersion="$version" -PnativecmpkitRepository="$candidate_repo" linkDebugFrameworkIosSimulatorArm64

app="$project/build/KiraWebViewConsumer.app"
frameworks="$project/build/bin/iosSimulatorArm64/debugFramework"
mkdir -p "$app"
xcrun --sdk iphonesimulator swiftc -parse-as-library -target arm64-apple-ios18.5-simulator \
    -sdk "$(xcrun --sdk iphonesimulator --show-sdk-path)" -F "$frameworks" -framework KiraWebViewConsumer \
    -framework WebKit "$project/Runner.swift" -o "$app/KiraWebViewConsumer"
cp "$project/Info.plist" "$app/Info.plist"
codesign --force --sign - "$app"

simulator="$(xcrun simctl list -j devices available | python3 -c '
import json, sys
runtimes = json.load(sys.stdin)["devices"]
ios_versions = {
    name: tuple(map(int, name.split(".iOS-")[-1].split("-")))
    for name in runtimes if ".iOS-" in name
}
for runtime in sorted(ios_versions, key=ios_versions.__getitem__, reverse=True):
    for device in runtimes[runtime]:
        if device["isAvailable"] and device["name"].startswith("iPhone"):
            print(device["udid"])
            sys.exit(0)
raise SystemExit("No available iPhone simulator")
')"
trap 'xcrun simctl shutdown "$simulator" 2>/dev/null || true' EXIT
xcrun simctl bootstatus "$simulator" -b
xcrun simctl install "$simulator" "$app"
python3 "$project/check_console.py" "$simulator" "$project/build/webview-console.log"
