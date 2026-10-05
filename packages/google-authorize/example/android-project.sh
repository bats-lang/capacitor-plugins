#!/bin/sh
# Makes the demo's Android project (android/, not committed) from
# Capacitor's template, as pwa makes an app's: Gradle pinned by its
# distribution's SHA-256, the debug build signed with the demo key
# (signing.gradle), the web app and the plugin synced in. Run after
# `corepack pnpm install --frozen-lockfile` and
# `corepack pnpm --recursive run build` at the repository's root.
set -eu
cd "$(dirname "$0")"
export COREPACK_ENABLE_DOWNLOAD_PROMPT=0

rm -rf android
corepack pnpm exec cap add android

# Gradle pinned by its distribution's SHA-256, as each plugin's own wrapper is
gradle_url='distributionUrl=https\://services.gradle.org/distributions/gradle-8.14.3-all.zip'
gradle_sha256='ed1a8d686605fd7c23bdf62c7fc7add1c5b23b2bbc3721e661934ef4a4911d7c'
properties=android/gradle/wrapper/gradle-wrapper.properties
if ! grep -qxF "$gradle_url" "$properties"; then
  echo "Capacitor's template asks for another Gradle than 8.14.3: pin its SHA-256 in android-project.sh" >&2
  grep distributionUrl "$properties" >&2
  exit 1
fi
echo "distributionSha256Sum=$gradle_sha256" >> "$properties"

printf "\napply from: '../../signing.gradle'\n" >> android/app/build.gradle
corepack pnpm exec cap sync android
