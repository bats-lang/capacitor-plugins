# @bats-lang/capacitor-google-authorize

Google authorization on Android with no sign-in: an access token for
OAuth scopes from Play services'
[`AuthorizationClient`](https://developers.google.com/android/reference/com/google/android/gms/auth/api/identity/AuthorizationClient),
silently once the reader has granted them. Its API is shaped as
Flutter's google_sign_in 7.x
[`GoogleSignInAuthorizationClient`](https://pub.dev/documentation/google_sign_in/latest/google_sign_in/GoogleSignInAuthorizationClient-class.html):
`authorizationForScopes` never shows anything, `authorizeScopes` may
show Google's consent screen. Android only.

No sign-in, no Credential Manager and no `GoogleSignIn`: the app is
identified by its package and signing certificate, through an Android
OAuth client in its Google Cloud project, so no client ID is passed.
There is no offline access (no server auth code).

## Install

```json
"@bats-lang/capacitor-google-authorize": "github:bats-lang/capacitor-plugins#<commit>&path:/packages/google-authorize"
```

with `nodeLinker: hoisted` in the app's `pnpm-workspace.yaml`, then
`pnpm exec cap sync android`. It uses `play-services-auth` 21.5.0 (an
app sets `playServicesAuthVersion` in its root `ext` to change it).

## Use

```ts
import { GoogleAuthorize } from '@bats-lang/capacitor-google-authorize';

const scopes = ['https://www.googleapis.com/auth/drive.appdata'];
const { authorization } = await GoogleAuthorize.authorizationForScopes({ scopes });
// null: ask when the reader acts
const granted = authorization ?? (await GoogleAuthorize.authorizeScopes({ scopes })).authorization;
```

When Google refuses a token (a 401), `clearAuthorizationToken` drops it
from Play services' cache before asking again.

## API

<docgen-index>

* [`authorizationForScopes(...)`](#authorizationforscopes)
* [`authorizeScopes(...)`](#authorizescopes)
* [`clearAuthorizationToken(...)`](#clearauthorizationtoken)
* [`revokeAccess(...)`](#revokeaccess)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Google authorization with no sign-in, over Play services' `AuthorizationClient` on Android, shaped as Flutter's
google_sign_in 7.x `GoogleSignInAuthorizationClient`. Android only: in a browser and on iOS each method rejects
with Capacitor's `UNIMPLEMENTED`.

### authorizationForScopes(...)

```typescript
authorizationForScopes(options: ScopesOptions) => Promise<AuthorizationAnswer>
```

The access token for the scopes when they are already granted, without showing anything
(`AuthorizationClient.authorize`, with no resolution). When the reader must consent first, nothing is shown and
the answer's `authorization` is `null`.

Rejects with the platform's code (`CommonStatusCodes`' name, such as `NETWORK_ERROR`), `UNEXPECTED` (an answer
the platform documents no meaning for, said in the message), or `INVALID_OPTIONS`.

| Param         | Type                                                    |
| ------------- | ------------------------------------------------------- |
| **`options`** | <code><a href="#scopesoptions">ScopesOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#authorizationanswer">AuthorizationAnswer</a>&gt;</code>

**Since:** 0.1.0

--------------------


### authorizeScopes(...)

```typescript
authorizeScopes(options: ScopesOptions) => Promise<GrantedAuthorization>
```

The access token for the scopes, showing Google's consent screen when the reader must consent first
(`AuthorizationClient.authorize`, then its resolution's intent, then `getAuthorizationResultFromIntent`).

Rejects with `CANCELED` when the reader backs out (Google's result says so, or the consent screen ends with
`RESULT_CANCELED` and returns nothing), `CONSENT_SHOWING` while another call's consent screen is showing, the
platform's code (`CommonStatusCodes`' name, such as `DEVELOPER_ERROR`, with its message, also when Google ends the
consent screen with it), `UNEXPECTED` (an answer the platform documents no meaning for, said in the message: a grant
with no access token, no scope or a blank account; a consent screen that ends with `RESULT_OK` or another result
code and returns nothing; one that could not be shown; a status code `CommonStatusCodes` does not name), or
`INVALID_OPTIONS`.

| Param         | Type                                                    |
| ------------- | ------------------------------------------------------- |
| **`options`** | <code><a href="#scopesoptions">ScopesOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#grantedauthorization">GrantedAuthorization</a>&gt;</code>

**Since:** 0.1.0

--------------------


### clearAuthorizationToken(...)

```typescript
clearAuthorizationToken(options: ClearOptions) => Promise<void>
```

Removes the access token from Play services' cache (`AuthorizationClient.clearToken`), so that the next
authorization gets a new one: for a token that Google refused.

Rejects with the platform's code, `UNEXPECTED` (said in the message), or `INVALID_OPTIONS`.

| Param         | Type                                                  |
| ------------- | ----------------------------------------------------- |
| **`options`** | <code><a href="#clearoptions">ClearOptions</a></code> |

**Since:** 0.1.0

--------------------


### revokeAccess(...)

```typescript
revokeAccess(options: RevokeOptions) => Promise<void>
```

Takes back the account's grant of the scopes (`AuthorizationClient.revokeAccess`).

Rejects with the platform's code, `UNEXPECTED` (said in the message), or `INVALID_OPTIONS`.

| Param         | Type                                                    |
| ------------- | ------------------------------------------------------- |
| **`options`** | <code><a href="#revokeoptions">RevokeOptions</a></code> |

**Since:** 0.1.0

--------------------


### Interfaces


#### AuthorizationAnswer

| Prop                | Type                                                    | Description                                                                                                                         | Since |
| ------------------- | ------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------- | ----- |
| **`authorization`** | <code><a href="#authorization">Authorization</a></code> | The authorization, or `null` when the reader must consent first. (A plugin call resolves with an object, never with `null` itself.) | 0.1.0 |


#### Authorization

| Prop                | Type                  | Description                                                                                                        | Since |
| ------------------- | --------------------- | ------------------------------------------------------------------------------------------------------------------ | ----- |
| **`accessToken`**   | <code>string</code>   | The access token.                                                                                                  | 0.1.0 |
| **`grantedScopes`** | <code>string[]</code> | The scopes the reader granted.                                                                                     | 0.1.0 |
| **`account`**       | <code>string</code>   | The Google account the grant is for (its name on Android, an email address), or `null` when the result names none. | 0.1.0 |


#### ScopesOptions

| Prop         | Type                  | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | Since |
| ------------ | --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----- |
| **`scopes`** | <code>string[]</code> | The OAuth scopes, at least one, none empty or blank (else the call rejects with `INVALID_OPTIONS`), such as `https://www.googleapis.com/auth/drive.appdata`. A non-empty scope Google does not recognise is not refused here: Play services' client library checks only that a scope is not empty (`Scope`, in play-services-basement, which play-services-auth 21.5.0 brings) and that the list is not empty (`AuthorizationRequest.Builder.setRequestedScopes`, play-services-auth 21.5.0), and sends it on. What Google answers for it is not documented, so it may be any answer the method documents; a grant names what was granted in `grantedScopes`. | 0.1.0 |


#### GrantedAuthorization

| Prop                | Type                                                    | Description        | Since |
| ------------------- | ------------------------------------------------------- | ------------------ | ----- |
| **`authorization`** | <code><a href="#authorization">Authorization</a></code> | The authorization. | 0.1.0 |


#### ClearOptions

| Prop              | Type                | Description                                                                                    | Since |
| ----------------- | ------------------- | ---------------------------------------------------------------------------------------------- | ----- |
| **`accessToken`** | <code>string</code> | The access token to remove, not empty or blank (else the call rejects with `INVALID_OPTIONS`). | 0.1.0 |


#### RevokeOptions

| Prop          | Type                  | Description                                                                                                                                    | Since |
| ------------- | --------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- | ----- |
| **`account`** | <code>string</code>   | The Google account whose grant is taken back: an authorization's `account`, not empty or blank (else the call rejects with `INVALID_OPTIONS`). | 0.1.0 |
| **`scopes`**  | <code>string[]</code> | The scopes to take back, at least one, none empty or blank (else the call rejects with `INVALID_OPTIONS`).                                     | 0.1.0 |

</docgen-api>
