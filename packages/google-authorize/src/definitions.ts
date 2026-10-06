/**
 * Google authorization with no sign-in, over Play services' `AuthorizationClient` on Android, shaped as Flutter's
 * google_sign_in 7.x `GoogleSignInAuthorizationClient`. Android only: in a browser and on iOS each method rejects
 * with Capacitor's `UNIMPLEMENTED`.
 */
export interface GoogleAuthorizePlugin {
  /**
   * The access token for the scopes when they are already granted, without showing anything
   * (`AuthorizationClient.authorize`, with no resolution). When the reader must consent first, nothing is shown and
   * the answer's `authorization` is `null`.
   *
   * Rejects with the platform's code (`CommonStatusCodes`' name, such as `NETWORK_ERROR`), `UNEXPECTED` (an answer
   * the platform documents no meaning for, said in the message), or `INVALID_OPTIONS`.
   *
   * @since 0.1.0
   */
  authorizationForScopes(options: ScopesOptions): Promise<AuthorizationAnswer>;

  /**
   * The access token for the scopes, showing Google's consent screen when the reader must consent first
   * (`AuthorizationClient.authorize`, then its resolution's intent, then `getAuthorizationResultFromIntent`).
   *
   * Rejects with `CANCELED` when the reader backs out (Google's result says so, or the consent screen ends with
   * `RESULT_CANCELED` and returns nothing), `CONSENT_SHOWING` while another call's consent screen is showing, the platform's code
   * (`CommonStatusCodes`' name, such as `DEVELOPER_ERROR`, with its message, also when Google ends the consent screen
   * with it), `UNEXPECTED` (an answer the platform documents no meaning for, such as a grant with no access token, no scope or a blank account, a
   * consent screen that ends with `RESULT_OK` or another result code and returns nothing, or a status code `CommonStatusCodes` does not name, said in the
   * message), or `INVALID_OPTIONS`.
   *
   * @since 0.1.0
   */
  authorizeScopes(options: ScopesOptions): Promise<GrantedAuthorization>;

  /**
   * Removes the access token from Play services' cache (`AuthorizationClient.clearToken`), so that the next
   * authorization gets a new one: for a token that Google refused.
   *
   * Rejects with the platform's code, `UNEXPECTED` (said in the message), or `INVALID_OPTIONS`.
   *
   * @since 0.1.0
   */
  clearAuthorizationToken(options: ClearOptions): Promise<void>;

  /**
   * Takes back the account's grant of the scopes (`AuthorizationClient.revokeAccess`).
   *
   * Rejects with the platform's code, `UNEXPECTED` (said in the message), or `INVALID_OPTIONS`.
   *
   * @since 0.1.0
   */
  revokeAccess(options: RevokeOptions): Promise<void>;
}

export interface ScopesOptions {
  /**
   * The OAuth scopes, at least one, none empty or blank (else the call rejects with `INVALID_OPTIONS`), such as
   * `https://www.googleapis.com/auth/drive.appdata`.
   *
   * @since 0.1.0
   */
  scopes: string[];
}

export interface Authorization {
  /**
   * The access token.
   *
   * @since 0.1.0
   */
  accessToken: string;

  /**
   * The scopes the reader granted.
   *
   * @since 0.1.0
   */
  grantedScopes: string[];

  /**
   * The Google account the grant is for (its name on Android, an email address), or `null` when the result names
   * none.
   *
   * @since 0.1.0
   */
  account: string | null;
}

export interface AuthorizationAnswer {
  /**
   * The authorization, or `null` when the reader must consent first. (A plugin call resolves with an object, never
   * with `null` itself.)
   *
   * @since 0.1.0
   */
  authorization: Authorization | null;
}

export interface GrantedAuthorization {
  /**
   * The authorization.
   *
   * @since 0.1.0
   */
  authorization: Authorization;
}

export interface ClearOptions {
  /**
   * The access token to remove, not empty or blank (else the call rejects with `INVALID_OPTIONS`).
   *
   * @since 0.1.0
   */
  accessToken: string;
}

export interface RevokeOptions {
  /**
   * The Google account whose grant is taken back: an authorization's `account`, not empty or blank (else the call
   * rejects with `INVALID_OPTIONS`).
   *
   * @since 0.1.0
   */
  account: string;

  /**
   * The scopes to take back, at least one, none empty or blank (else the call rejects with `INVALID_OPTIONS`).
   *
   * @since 0.1.0
   */
  scopes: string[];
}
