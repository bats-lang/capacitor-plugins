#!/bin/sh
# usage: build-demo.sh <demo.jks> <its password>
# Builds the demo's debug APK, signed with quire#316's throwaway demo key
# (on quire's branch demo/google-sign-in-authorize), into
# android/app/build/outputs/apk/debug/app-debug.apk. Needs Node 22, a
# JDK 21 and the Android SDK (ANDROID_HOME).
set -eu
KEYSTORE=$(realpath "$1")
PASSWORD=$2
cd "$(dirname "$0")"
export COREPACK_ENABLE_DOWNLOAD_PROMPT=0
PNPM="corepack pnpm@12.9.1"
rm -rf android
$PNPM install
$PNPM exec cap add android
cat >> android/app/build.gradle <<GRADLE

android {
    signingConfigs {
        debug {
            storeFile file('$KEYSTORE')
            storePassword '$PASSWORD'
            keyAlias 'demo'
            keyPassword '$PASSWORD'
        }
    }
}
GRADLE
$PNPM exec cap sync android
cd android
./gradlew assembleDebug
