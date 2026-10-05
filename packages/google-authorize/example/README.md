# google-authorize demo

A small app to check `google-authorize` on a phone
(bats-lang/quire#321, Phase 0 step 3): a button for each of the plugin's
calls, and for the two requests an app makes with the token, each call
and its answer logged with a timestamp. It asks for `drive.appdata`
alone.

- **authorizationForScopes** and **authorizeScopes**: each token is
  numbered, and one handed back again says `SAME as token #n`; the
  answer's `account` (or `null (ABSENT)`) and `grantedScopes` are shown.
- **Check token**: Google's `tokeninfo` for the last token (its `scope`
  and `expires_in`, or its error).
- **Account address**: Drive's `about.get?fields=user/emailAddress` with
  the last token.
- **clearAuthorizationToken**: the last token.
- **revokeAccess**: the last authorization's `account`.
- **Copy log**: the log, oldest first, to paste into an issue.

It is a Vite app (`src/main.ts`) with the plugin from the workspace, its
package `io.github.batslang.googlesignindemo`.

## Getting the APK

`.github/workflows/demo-apk.yml` builds it on each change to
`packages/google-authorize/` on main, and by hand (Run workflow):

- the prerelease `demo-google-authorize`, whose asset
  `google-authorize-demo.apk` is the APK itself:
  https://github.com/bats-lang/capacitor-plugins/releases/download/demo-google-authorize/google-authorize-demo.apk
- the run's artifact `google-authorize-demo-apk`, a zip holding the APK.

Android installs it once the browser (or Files) is allowed to install
unknown apps.

## The demo key

The APK is signed with a throwaway key made only for this demo; it signs
nothing else, and is public on purpose, so that the APK has the SHA-1 the
Android OAuth client of bats-lang/quire#320 is made for (Google matches
an Android client by package and signing certificate):

- `demo.jks` (PKCS12), alias `demo`
- store password and key password: `g5jwKdpyVSP_qUndX1wglzKQ`
- DN `CN=Quire sign-in demo`, valid 2026-10-05 to 2036-10-02
- SHA-1 `7F:D1:24:96:68:77:49:DE:90:B0:BB:22:D0:4E:60:67:BB:61:80:B9`
- SHA-256 `42:3B:47:08:7C:29:7E:DD:ED:CF:C7:50:F6:FC:B9:B8:60:BB:18:1E:75:C1:5D:6D:58:43:C8:4A:2F:33:DB:40`

It was made for quire#316's demo (quire's branch
`demo/google-sign-in-authorize`), under the same package.

## Building it here

At the repository's root, with JDK 21 and the Android SDK
(`ANDROID_HOME`, with `platforms;android-36` and `build-tools;36.0.0`):

```sh
corepack pnpm install --frozen-lockfile
corepack pnpm --recursive run build
packages/google-authorize/example/build-apk.sh
```

`build-apk.sh` makes the Android project from Capacitor's template
(`android-project.sh`: Gradle pinned by its SHA-256, the debug build
signed by `signing.gradle`), builds
`android/app/build/outputs/apk/debug/app-debug.apk`, and checks it
(`verify-apk.sh`: signed by the demo key alone, the package, and
GoogleAuthorize registered). It is a debug build, so the page can be
inspected from a computer at `chrome://inspect`.
