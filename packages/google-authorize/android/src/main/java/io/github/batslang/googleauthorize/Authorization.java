package io.github.batslang.googleauthorize;

import java.util.List;

/** An access token for the scopes granted, as Play services' AuthorizationResult gives it. */
final class Authorization {

    final String accessToken;
    final List<String> grantedScopes;
    /** The account the grant is for (its name, an email address), or null when the result names none. */
    final String account;

    Authorization(String accessToken, List<String> grantedScopes, String account) {
        this.accessToken = accessToken;
        this.grantedScopes = grantedScopes;
        this.account = account;
    }
}
