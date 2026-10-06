package io.github.batslang.googleauthorize;

import java.util.ArrayList;
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
    /** Activity.RESULT_OK, as Android defines it (GoogleAuthorize uses no Android class). */
    static final int RESULT_OK = -1;
    /** Activity.RESULT_CANCELED, as Android defines it. */
    static final int RESULT_CANCELED = 0;
    /**
     * An answer the platform documents no meaning for (an exception that is not an ApiException, a status code
     * CommonStatusCodes does not name, a grant with no access token): never folded into a known code. Its message says
     * what it was.
     */
    static final String UNEXPECTED = "UNEXPECTED";
    /** An option is missing or is not what the method takes. */
    static final String INVALID_OPTIONS = "INVALID_OPTIONS";
    /** What INVALID_OPTIONS says of scopes. */
    static final String INVALID_SCOPES = "scopes must be a non-empty array of non-blank strings";

    private final AuthorizationService<Consent, Returned> service;
    private final ConsentScreen<Consent> consentScreen;
    /** The authorizeScopes call whose consent screen is showing, or null. */
    private Answer waiting;

    GoogleAuthorize(AuthorizationService<Consent, Returned> service, ConsentScreen<Consent> consentScreen) {
        this.service = service;
        this.consentScreen = consentScreen;
    }

    /** What INVALID_OPTIONS says of an access token. */
    static final String INVALID_TOKEN = "accessToken must be a non-blank string";
    /** What INVALID_OPTIONS says of an account. */
    static final String INVALID_ACCOUNT = "account must be a non-blank string";

    /**
     * Whether scopes are what the methods take: a list (null when the call gave no array) of at least one scope, each
     * a non-blank string (an item of another type, a number or JSON null, is not one). An empty scope would make Play services' Scope throw; a blank one is no scope
     * (an OAuth scope has no space in it), though Play services would send it on. Either is the plugin's answer to its
     * options, not the platform's to a request.
     */
    static boolean scopesValid(List<?> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return false;
        }
        for (Object scope : scopes) {
            if (!(scope instanceof String name) || name.isBlank()) {
                return false;
            }
        }
        return true;
    }

    /** Scopes scopesValid took, as the strings they are. */
    private static List<String> namesOf(List<?> scopes) {
        List<String> names = new ArrayList<>();
        for (Object scope : scopes) {
            names.add((String) scope);
        }
        return names;
    }

    /** The token when the scopes are granted, else no authorization; never shows anything. */
    void authorizationForScopes(List<?> scopes, Answer answer) {
        if (!scopesValid(scopes)) {
            answer.failed(new Failure(INVALID_OPTIONS, INVALID_SCOPES));
            return;
        }
        service.authorize(
            namesOf(scopes),
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
    void authorizeScopes(List<?> scopes, Answer answer) {
        if (!scopesValid(scopes)) {
            answer.failed(new Failure(INVALID_OPTIONS, INVALID_SCOPES));
            return;
        }
        service.authorize(
            namesOf(scopes),
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

    /**
     * A grant answers with its token, the scopes granted and its account (or null when it names none). One missing what
     * a grant must hold is unexpected: no token (null, empty or blank), no scope granted or a blank one, or an account
     * that is named but blank.
     */
    private static void grantedOrUnexpected(Authorization authorization, Answer answer) {
        if (!nonBlank(authorization.accessToken)) {
            answer.failed(new Failure(UNEXPECTED, "The authorization result has no access token"));
            return;
        }
        if (!scopesValid(authorization.grantedScopes)) {
            answer.failed(new Failure(UNEXPECTED, "The authorization result grants no scope, or a blank one"));
            return;
        }
        if (authorization.account != null && authorization.account.isBlank()) {
            answer.failed(new Failure(UNEXPECTED, "The authorization result names a blank account"));
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
        try {
            consentScreen.show(consent);
        } catch (RuntimeException failure) {
            // Not shown (androidx throws IllegalStateException for a launcher not registered): answered now, so no
            // later call is refused as CONSENT_SHOWING
            waiting = null;
            answer.failed(
                new Failure(
                    UNEXPECTED,
                    "The consent screen could not be shown: " +
                        failure.getClass().getSimpleName() +
                        ": " +
                        failure.getMessage()
                )
            );
        }
    }

    /**
     * The consent screen's result: its result code, the intent it returned (or null), and why it could not be shown
     * (androidx's answer to a launch that threw), or null when it was. A screen never shown is UNEXPECTED. Whatever the
     * result code, a returned intent is read (getAuthorizationResultFromIntent), so what Google says is the answer: a
     * grant, its CANCELED when the reader backed out (Play services answers CANCELED for an intent with no status), or
     * any other status, such as DEVELOPER_ERROR, which also ends the screen with RESULT_CANCELED. With no intent there is
     * nothing to read: with RESULT_CANCELED the reader backed out (Android's convention); with RESULT_OK, or any other
     * code, the answer is UNEXPECTED, naming the code.
     */
    void consentEnded(int resultCode, Returned returned, String launchFailure) {
        Answer answer = waiting;
        waiting = null;
        if (answer == null) {
            return;
        }
        if (launchFailure != null) {
            // Never shown: Google said nothing, and the reader did not back out
            answer.failed(new Failure(UNEXPECTED, "The consent screen could not be shown: " + launchFailure));
            return;
        }
        if (returned == null) {
            if (resultCode == RESULT_CANCELED) {
                answer.failed(new Failure(CANCELED, "The reader backed out of the consent screen"));
            } else if (resultCode == RESULT_OK) {
                // RESULT_OK with nothing to read: no answer Play services documents
                answer.failed(new Failure(UNEXPECTED, "The consent screen completed but returned nothing"));
            } else {
                answer.failed(
                    new Failure(
                        UNEXPECTED,
                        "The consent screen ended with result code " + resultCode + " and returned nothing"
                    )
                );
            }
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

    /** Whether text is a non-blank string (null when the call gave none). */
    static boolean nonBlank(String text) {
        return text != null && !text.isBlank();
    }

    void clearAuthorizationToken(String accessToken, Answer answer) {
        if (!nonBlank(accessToken)) {
            answer.failed(new Failure(INVALID_OPTIONS, INVALID_TOKEN));
            return;
        }
        service.clearToken(accessToken, doneOrFailed(answer));
    }

    void revokeAccess(String account, List<?> scopes, Answer answer) {
        if (!nonBlank(account)) {
            answer.failed(new Failure(INVALID_OPTIONS, INVALID_ACCOUNT));
            return;
        }
        if (!scopesValid(scopes)) {
            answer.failed(new Failure(INVALID_OPTIONS, INVALID_SCOPES));
            return;
        }
        service.revokeAccess(account, namesOf(scopes), doneOrFailed(answer));
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
