#!/bin/sh
# Builds the demo's APK, signed with the demo key, into
# android/app/build/outputs/apk/debug/app-debug.apk, and checks it. Run
# after `corepack pnpm install --frozen-lockfile` and
# `corepack pnpm --recursive run build` at the repository's root; needs
# JDK 21 and the Android SDK (ANDROID_HOME, with platforms;android-36 and
# build-tools;36.0.0). CI runs the same steps (demo-apk.yml).
set -eu
cd "$(dirname "$0")"
./android-project.sh
(cd android && ./gradlew --no-daemon --stacktrace assembleDebug)
./verify-apk.sh android/app/build/outputs/apk/debug/app-debug.apk
