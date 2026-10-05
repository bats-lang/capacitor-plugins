#!/bin/sh
# usage: verify-apk.sh <apk>
# Fails unless the APK is the demo's: signed by the demo key alone (the
# SHA-1 of bats-lang/quire#320's OAuth client), package
# io.github.batslang.googlesignindemo, with GoogleAuthorize registered.
set -eu
apk=$1
build_tools="$ANDROID_HOME/build-tools/36.0.0"
expected_sha1='7fd12496687749de90b0bb22d04e6067bb6180b9'
expected_package='io.github.batslang.googlesignindemo'
expected_plugin='io.github.batslang.googleauthorize.GoogleAuthorizePlugin'

certificates=$("$build_tools/apksigner" verify --print-certs "$apk")
echo "$certificates"
signers=$(echo "$certificates" | grep -c '^Signer #[0-9]* certificate SHA-1 digest: ' || true)
sha1=$(echo "$certificates" | sed -n 's/^Signer #1 certificate SHA-1 digest: //p')
if [ "$signers" != 1 ] || [ "$sha1" != "$expected_sha1" ]; then
  echo "The APK is signed by $signers signer(s), the first with SHA-1 '$sha1', not by the demo key alone ($expected_sha1)" >&2
  exit 1
fi

package=$("$build_tools/aapt" dump badging "$apk" | sed -n "s/^package: name='\([^']*\)'.*/\1/p")
if [ "$package" != "$expected_package" ]; then
  echo "The APK's package is '$package', not $expected_package" >&2
  exit 1
fi

plugins=$(unzip -p "$apk" assets/capacitor.plugins.json)
echo "$plugins"
if ! echo "$plugins" | grep -qF "\"$expected_plugin\""; then
  echo "The APK does not register $expected_plugin" >&2
  exit 1
fi
echo "The APK is signed by the demo key alone (SHA-1 $expected_sha1), its package is $expected_package, and it registers GoogleAuthorize"
