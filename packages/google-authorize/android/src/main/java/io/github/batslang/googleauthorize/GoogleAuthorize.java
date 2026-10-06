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

    /** The reader backed out of the consent screen, as Play services' CommonStatusCodes names it. */
    static final String CANCELED = "CANCELED";
    /** authorizeScopes was called while another call's consent screen was showing. */
    static final String CONSENT_SHOWING = "CONSENT_SHOWING";
    /**
     * An answer the platform documents no meaning for (an exception that is not an ApiException, a status code
     * CommonStatusCodes does not name, a grant with no access token): never folded into a known code. Its message says
     * what it was.
     */
    static final String UNEXPECTED = "UNEXPECTED";

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
                        grantedOrUnexpected(granted.authorization, answer);
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
                        grantedOrUnexpected(granted.authorization, answer);
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

    /** A grant answers with its token; one with no token (AuthorizationResult.getAccessToken is null) is unexpected. */
    private static void grantedOrUnexpected(Authorization authorization, Answer answer) {
        if (authorization.accessToken == null || authorization.accessToken.isEmpty()) {
            answer.failed(new Failure(UNEXPECTED, "The authorization result has no access token"));
            return;
        }
        answer.authorized(authorization);
    }

    private void showConsent(Consent consent, Answer answer) {
        if (waiting != null) {
            answer.failed(new Failure(CONSENT_SHOWING, "Another call's consent screen is showing"));
            return;
        }
        waiting = answer;
        consentScreen.show(consent);
    }

    /**
     * The consent screen's result: whether it completed (RESULT_OK), and the intent it returned, or null. Whatever the
     * result code, a returned intent is read (getAuthorizationResultFromIntent), so what Google says is the answer: a
     * grant, its CANCELED when the reader backed out (Play services answers CANCELED for an intent with no status), or
     * any other status, such as DEVELOPER_ERROR, which also ends the screen with RESULT_CANCELED. With no intent and no
     * RESULT_OK there is nothing to read, and the reader backed out.
     */
    void consentEnded(boolean completed, Returned returned) {
        Answer answer = waiting;
        waiting = null;
        if (answer == null) {
            return;
        }
        if (!completed && returned == null) {
            answer.failed(new Failure(CANCELED, "The reader backed out of the consent screen"));
            return;
        }
        service.authorizationFromConsent(
            returned,
            new Reply<>() {
                @Override
                public void succeeded(Authorization authorization) {
                    grantedOrUnexpected(authorization, answer);
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
