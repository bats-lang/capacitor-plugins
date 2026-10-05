package io.github.batslang.googleauthorize;

import java.util.List;

/**
 * The plugin's logic, over an AuthorizationService and a ConsentScreen, shaped as Flutter's google_sign_in 7.x
 * authorization client: authorizationForScopes never shows anything, authorizeScopes may show Google's consent,
 * clearAuthorizationToken drops a token from Play services' cache, and revokeAccess takes a grant back.
 *
 * <p>The waiting call is read and written only on the main thread, where the service's replies and the consent
 * screen's result arrive.
 */
final class GoogleAuthorize<Consent, Returned> {

    /** The reader backed out of the consent screen. */
    static final String CANCELED = "CANCELED";
    /** authorizeScopes was called while another call's consent screen was showing. */
    static final String CONSENT_SHOWING = "CONSENT_SHOWING";

    private final AuthorizationService<Consent, Returned> service;
    private final ConsentScreen<Consent> consentScreen;
    /** The authorizeScopes call whose consent screen is showing, or null. */
    private Answer waiting;

    GoogleAuthorize(AuthorizationService<Consent, Returned> service, ConsentScreen<Consent> consentScreen) {
        this.service = service;
        this.consentScreen = consentScreen;
    }

    /** The token when the scopes are granted, else no authorization; never shows anything. */
    void authorizationForScopes(List<String> scopes, Answer answer) {
        service.authorize(
            scopes,
            new Reply<>() {
                @Override
                public void succeeded(Authorizing<Consent> authorizing) {
                    if (authorizing instanceof Authorizing.Granted<Consent> granted) {
                        answer.authorized(granted.authorization);
                    } else {
                        answer.notAuthorized();
                    }
                }

                @Override
                public void failed(Failure failure) {
                    answer.failed(failure);
                }
            }
        );
    }

    /** The token when the scopes are granted, else after the reader consents on Google's screen. */
    void authorizeScopes(List<String> scopes, Answer answer) {
        service.authorize(
            scopes,
            new Reply<>() {
                @Override
                public void succeeded(Authorizing<Consent> authorizing) {
                    if (authorizing instanceof Authorizing.Granted<Consent> granted) {
                        answer.authorized(granted.authorization);
                    } else {
                        showConsent(((Authorizing.ConsentNeeded<Consent>) authorizing).consent, answer);
                    }
                }

                @Override
                public void failed(Failure failure) {
                    answer.failed(failure);
                }
            }
        );
    }

    private void showConsent(Consent consent, Answer answer) {
        if (waiting != null) {
            answer.failed(new Failure(CONSENT_SHOWING, "Another call's consent screen is showing"));
            return;
        }
        waiting = answer;
        consentScreen.show(consent);
    }

    /** The consent screen's result: completed (RESULT_OK) with what it returned, or not (the reader backed out). */
    void consentEnded(boolean completed, Returned returned) {
        Answer answer = waiting;
        waiting = null;
        if (answer == null) {
            return;
        }
        if (!completed) {
            answer.failed(new Failure(CANCELED, "The reader backed out of the consent screen"));
            return;
        }
        service.authorizationFromConsent(
            returned,
            new Reply<>() {
                @Override
                public void succeeded(Authorization authorization) {
                    answer.authorized(authorization);
                }

                @Override
                public void failed(Failure failure) {
                    answer.failed(failure);
                }
            }
        );
    }

    void clearAuthorizationToken(String accessToken, Answer answer) {
        service.clearToken(accessToken, doneOrFailed(answer));
    }

    void revokeAccess(String account, List<String> scopes, Answer answer) {
        service.revokeAccess(account, scopes, doneOrFailed(answer));
    }

    private static Reply<Void> doneOrFailed(Answer answer) {
        return new Reply<>() {
            @Override
            public void succeeded(Void nothing) {
                answer.done();
            }

            @Override
            public void failed(Failure failure) {
                answer.failed(failure);
            }
        };
    }
}
