// The google-authorize demo (bats-lang/quire#321, Phase 0 step 3): a button
// for each call of the device check, and a timestamped log line for each
// call and its answer.
import { Capacitor } from '@capacitor/core';
import { GoogleAuthorize } from '@bats-lang/capacitor-google-authorize';
import type { Authorization } from '@bats-lang/capacitor-google-authorize';

const scopes = ['https://www.googleapis.com/auth/drive.appdata'];

// Every log line, oldest first (the screen shows them newest first)
const lines: string[] = [];
// Every access token seen since the app started, to tell a new one from one handed back again
const tokensSeen: string[] = [];
// The last authorization answered, whose token and account the other buttons use
let lastAuthorization: Authorization | null = null;

function element(id: string): HTMLElement {
  const found = document.getElementById(id);
  if (!found) throw new Error('no element ' + id);
  return found;
}

function log(line: string): void {
  lines.push(new Date().toISOString() + '  ' + line);
  element('log').textContent = lines.slice().reverse().join('\n');
}

function tokenShown(token: string): string {
  return token.slice(0, 12) + '...';
}

// "token #2 (new)" or "token SAME as token #1", for each answer's token
function tokenLabel(token: string): string {
  const before = tokensSeen.indexOf(token);
  if (before >= 0) return 'token SAME as token #' + (before + 1) + ' ' + tokenShown(token);
  tokensSeen.push(token);
  return 'token #' + tokensSeen.length + ' (new) ' + tokenShown(token);
}

function authorizationLines(authorization: Authorization): string {
  return (
    '-> ' +
    tokenLabel(authorization.accessToken) +
    '\n   account: ' +
    (authorization.account === null ? 'null (ABSENT)' : authorization.account) +
    '\n   grantedScopes: ' +
    authorization.grantedScopes.join(' ')
  );
}

function failure(error: unknown): string {
  const { code, message } = error as { code?: string; message?: string };
  return '-> rejected, code ' + code + ': ' + message;
}

async function authorizationForScopes(): Promise<void> {
  log('authorizationForScopes()');
  try {
    const { authorization } = await GoogleAuthorize.authorizationForScopes({ scopes });
    if (authorization === null) {
      log('-> authorization null (consent needed; nothing shown)');
      return;
    }
    lastAuthorization = authorization;
    log(authorizationLines(authorization));
  } catch (error) {
    log(failure(error));
  }
}

async function authorizeScopes(): Promise<void> {
  log('authorizeScopes()');
  try {
    const { authorization } = await GoogleAuthorize.authorizeScopes({ scopes });
    lastAuthorization = authorization;
    log(authorizationLines(authorization));
  } catch (error) {
    log(failure(error));
  }
}

function lastToken(): string | null {
  if (lastAuthorization === null) log('no token yet: authorize first');
  return lastAuthorization === null ? null : lastAuthorization.accessToken;
}

async function checkToken(): Promise<void> {
  const token = lastToken();
  if (token === null) return;
  log('tokeninfo for ' + tokenShown(token));
  try {
    const response = await fetch('https://oauth2.googleapis.com/tokeninfo?access_token=' + encodeURIComponent(token));
    const info = (await response.json()) as Record<string, unknown>;
    log(
      '-> HTTP ' +
        response.status +
        (info.error === undefined
          ? ': scope ' + info.scope + ', expires_in ' + info.expires_in
          : ': error ' + info.error + (info.error_description ? ' (' + info.error_description + ')' : '')),
    );
  } catch (error) {
    log('-> tokeninfo failed: ' + error);
  }
}

async function accountAddress(): Promise<void> {
  const token = lastToken();
  if (token === null) return;
  log('Drive about.get?fields=user/emailAddress with ' + tokenShown(token));
  try {
    const response = await fetch('https://www.googleapis.com/drive/v3/about?fields=user/emailAddress', {
      headers: { Authorization: 'Bearer ' + token },
    });
    log('-> HTTP ' + response.status + ': ' + (await response.text()).replace(/\s+/g, ' '));
  } catch (error) {
    log('-> about.get failed: ' + error);
  }
}

async function clearAuthorizationToken(): Promise<void> {
  const token = lastToken();
  if (token === null) return;
  log('clearAuthorizationToken(' + tokenShown(token) + ')');
  try {
    await GoogleAuthorize.clearAuthorizationToken({ accessToken: token });
    log('-> cleared');
  } catch (error) {
    log(failure(error));
  }
}

async function revokeAccess(): Promise<void> {
  if (lastAuthorization === null) {
    log('no account yet: authorize first');
    return;
  }
  const account = lastAuthorization.account;
  if (account === null) {
    log('revokeAccess not called: the last authorization has no account (null)');
    return;
  }
  log('revokeAccess(' + account + ')');
  try {
    await GoogleAuthorize.revokeAccess({ account, scopes });
    log('-> revoked');
  } catch (error) {
    log(failure(error));
  }
}

async function copyLog(): Promise<void> {
  try {
    await navigator.clipboard.writeText(lines.join('\n'));
    log('log copied');
  } catch (error) {
    log('copy failed: ' + error);
  }
}

element('commit').textContent = import.meta.env.VITE_DEMO_COMMIT ?? 'a local build';
element('authorization-for-scopes').addEventListener('click', authorizationForScopes);
element('authorize-scopes').addEventListener('click', authorizeScopes);
element('check-token').addEventListener('click', checkToken);
element('account-address').addEventListener('click', accountAddress);
element('clear-authorization-token').addEventListener('click', clearAuthorizationToken);
element('revoke-access').addEventListener('click', revokeAccess);
element('copy-log').addEventListener('click', copyLog);
element('clear-log').addEventListener('click', () => {
  lines.length = 0;
  element('log').textContent = '';
});
log(
  'started: GoogleAuthorize ' +
    (Capacitor.isPluginAvailable('GoogleAuthorize') ? 'available' : 'NOT available') +
    ' on ' +
    Capacitor.getPlatform(),
);
