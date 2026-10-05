package io.github.batslang.googleauthorize;

import java.util.List;

/**
 * What the plugin uses of Play services' AuthorizationClient, each method one of its calls, so that the plugin's logic
 * is tested with a stand-in. Consent is what shows the consent screen (a PendingIntent), and Returned what that screen
 * gives back (an Intent).
 */
interface AuthorizationService<Consent, Returned> {
    /** AuthorizationClient.authorize, for these scopes. */
    void authorize(List<String> scopes, Reply<Authorizing<Consent>> reply);

    /** AuthorizationClient.getAuthorizationResultFromIntent, once the consent screen has given back its result. */
    void authorizationFromConsent(Returned returned, Reply<Authorization> reply);

    /** AuthorizationClient.clearToken. */
    void clearToken(String accessToken, Reply<Void> reply);

    /** AuthorizationClient.revokeAccess, for the account (its name) and these scopes. */
    void revokeAccess(String account, List<String> scopes, Reply<Void> reply);
}
