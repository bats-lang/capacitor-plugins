package io.github.batslang.googleauthorize;

/**
 * What AuthorizationClient.authorize answers: either the scopes are granted, or the reader must consent first (the
 * result has a resolution, whose consent screen is a Consent).
 */
abstract class Authorizing<Consent> {

    private Authorizing() {}

    static final class Granted<Consent> extends Authorizing<Consent> {

        final Authorization authorization;

        Granted(Authorization authorization) {
            this.authorization = authorization;
        }
    }

    static final class ConsentNeeded<Consent> extends Authorizing<Consent> {

        final Consent consent;

        ConsentNeeded(Consent consent) {
            this.consent = consent;
        }
    }
}
