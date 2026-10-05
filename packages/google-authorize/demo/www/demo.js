// Proof of concept (bats-lang/quire#321, Phase 0, step 3)
const scopes = ['https://www.googleapis.com/auth/drive.appdata'];
// quire's Web client ID (scripts/sync-clients.env)
const clientId = '162611675413-0501vhs5c72jqbu1e2brlj7tcf9rv5og.apps.googleusercontent.com';
let token = null;

function log(line) {
  const time = new Date().toISOString().slice(11, 23);
  const element = document.getElementById('log');
  element.textContent = time + ' ' + line + '\n' + element.textContent;
}

function plugin() {
  return window.Capacitor.Plugins.GoogleAuthorize;
}

// every token seen, to tell whether a call hands back a cached one
const seen = [];

function tokenLine(authorization) {
  const token = authorization.accessToken;
  const before = seen.indexOf(token);
  if (before < 0) seen.push(token);
  return 'token ' + token.slice(0, 8) + '... (' + (before < 0 ? 'new' : 'SAME as token #' + (before + 1)) + '), email ' + authorization.email;
}

async function authorize(prompting) {
  const method = prompting ? 'authorizeScopes' : 'authorizationForScopes';
  log(method + '()');
  try {
    const answer = await plugin()[method]({ clientId, scopes });
    if (answer.authorization) {
      token = answer.authorization.accessToken;
      log('-> ' + tokenLine(answer.authorization));
    } else {
      log('-> null (consent needed)');
    }
  } catch (error) {
    log('-> rejected, code ' + error.code + ': ' + error.message);
  }
}

async function clearToken() {
  if (!token) return log('no token yet');
  log('clearAuthorizationToken(' + token.slice(0, 8) + '...)');
  try {
    await plugin().clearAuthorizationToken({ accessToken: token });
    log('-> cleared');
  } catch (error) {
    log('-> rejected, code ' + error.code + ': ' + error.message);
  }
}

async function checkToken() {
  if (!token) return log('no token yet');
  const response = await fetch('https://oauth2.googleapis.com/tokeninfo?access_token=' + encodeURIComponent(token));
  const info = await response.json();
  log('tokeninfo ' + response.status + ': scope ' + info.scope + ', expires_in ' + info.expires_in + (info.error ? ', error ' + info.error : ''));
}

async function accountAddress() {
  if (!token) return log('no token yet');
  const response = await fetch('https://www.googleapis.com/drive/v3/about?fields=user/emailAddress', {
    headers: { Authorization: 'Bearer ' + token },
  });
  log('about.get ' + response.status + ': ' + (await response.text()).replace(/\s+/g, ' '));
}

async function revoke() {
  log('revoke()');
  try {
    await plugin().revoke({ clientId, scopes });
    token = null;
    log('-> revoked');
  } catch (error) {
    log('-> rejected, code ' + error.code + ': ' + error.message);
  }
}

document.getElementById('silent').addEventListener('click', () => authorize(false));
document.getElementById('interactive').addEventListener('click', () => authorize(true));
document.getElementById('check').addEventListener('click', () => checkToken().catch((error) => log('tokeninfo failed: ' + error)));
document.getElementById('account').addEventListener('click', () => accountAddress().catch((error) => log('about.get failed: ' + error)));
document.getElementById('clear-token').addEventListener('click', clearToken);
document.getElementById('revoke').addEventListener('click', revoke);
document.getElementById('clear').addEventListener('click', () => { document.getElementById('log').textContent = ''; });
log('ready: plugin ' + (window.Capacitor && window.Capacitor.isPluginAvailable('GoogleAuthorize') ? 'available' : 'NOT available'));
