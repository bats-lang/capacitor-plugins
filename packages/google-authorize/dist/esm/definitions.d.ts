export interface ScopesOptions {
    /** The Web client ID (not used by Android's AuthorizationClient without offline access). */
    clientId: string;
    /** The OAuth scopes, e.g. 'https://www.googleapis.com/auth/drive.appdata'. */
    scopes: string[];
}
export interface Authorization {
    accessToken: string;
    /** Proof of concept only: the account the grant is for, when Android names it. */
    email: string | null;
}
export interface GoogleAuthorizePlugin {
    /** Never shows UI: the token once the scopes are granted, else null. */
    authorizationForScopes(options: ScopesOptions): Promise<{
        authorization: Authorization | null;
    }>;
    /** May show Google's consent; rejects CANCELED, or with the platform's code. */
    authorizeScopes(options: ScopesOptions): Promise<{
        authorization: Authorization;
    }>;
    /** Drops a token (one Google refused) from Play services' cache (clearToken). */
    clearAuthorizationToken(options: {
        accessToken: string;
    }): Promise<void>;
    /** Takes back the grant of the scopes (AuthorizationClient.revokeAccess). */
    revoke(options: ScopesOptions): Promise<void>;
}
