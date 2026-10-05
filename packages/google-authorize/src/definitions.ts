export interface AuthorizeOptions {
  /** The Web client ID (not used by Android's AuthorizationClient without offline access). */
  clientId: string;
  /** The OAuth scopes, e.g. 'https://www.googleapis.com/auth/drive.appdata'. */
  scopes: string[];
  /** false: never show UI, reject NEEDS_INTERACTION when consent is needed. */
  interactive: boolean;
}

export interface AuthorizeResult {
  accessToken: string;
  /** Proof of concept only: the account the grant is for, when Android names it. */
  email: string | null;
}

export interface GoogleAuthorizePlugin {
  /** Rejects with code NEEDS_INTERACTION, CANCELED, or the platform's code. */
  authorize(options: AuthorizeOptions): Promise<AuthorizeResult>;
  /** Takes back the grant of the scopes (AuthorizationClient.revokeAccess). */
  revoke(options: { clientId: string; scopes: string[] }): Promise<void>;
}
