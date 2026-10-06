package io.github.batslang.googleauthorize;

import android.accounts.Account;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import com.google.android.gms.auth.api.identity.AuthorizationClient;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.ClearTokenRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.RevokeAccessRequest;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Scope;
import java.util.ArrayList;
import java.util.List;

/** The AuthorizationService of Play services' AuthorizationClient (play-services-auth). */
final class PlayAuthorizationService implements AuthorizationService<PendingIntent, Intent> {

    /** The type of a Google account on Android. */
    private static final String GOOGLE_ACCOUNT_TYPE = "com.google";

    private final Activity activity;

    PlayAuthorizationService(Activity activity) {
        this.activity = activity;
    }

    private AuthorizationClient client() {
        return Identity.getAuthorizationClient(activity);
    }

    private static List<Scope> scopesOf(List<String> names) {
        List<Scope> scopes = new ArrayList<>();
        for (String name : names) {
            scopes.add(new Scope(name));
        }
        return scopes;
    }

    /** What CommonStatusCodes.getStatusCodeString answers for a status code it does not name. */
    private static final String UNNAMED_STATUS = "unknown status code";

    /**
     * A failure: an ApiException's status as CommonStatusCodes names it, with the exception's message. Anything else
     * (another exception, or a status code CommonStatusCodes does not name) is UNEXPECTED, its message saying what it
     * was, never folded into a known code.
     */
    static Failure failureOf(Exception exception) {
        if (exception instanceof ApiException apiException) {
            int status = apiException.getStatusCode();
            String code = CommonStatusCodes.getStatusCodeString(status);
            if (!code.startsWith(UNNAMED_STATUS)) {
                return new Failure(code, String.valueOf(exception.getMessage()));
            }
            return new Failure(
                GoogleAuthorize.UNEXPECTED,
                "Status code " + status + ": " + String.valueOf(exception.getMessage())
            );
        }
        return new Failure(
            GoogleAuthorize.UNEXPECTED,
            exception.getClass().getSimpleName() + ": " + String.valueOf(exception.getMessage())
        );
    }

    // toGoogleSignInAccount is deprecated with GoogleSignIn, but it is the one way an AuthorizationResult names its
    // account (still there in play-services-auth 22.0.0), and revokeAccess needs that account
    @SuppressWarnings("deprecation")
    private static Authorization authorizationOf(AuthorizationResult result) {
        GoogleSignInAccount signedIn = result.toGoogleSignInAccount();
        Account account = signedIn == null ? null : signedIn.getAccount();
        return new Authorization(
            result.getAccessToken(),
            new ArrayList<>(result.getGrantedScopes()),
            account == null ? null : account.name
        );
    }

    @Override
    public void authorize(List<String> scopes, Reply<Authorizing<PendingIntent>> reply) {
        AuthorizationRequest request;
        try {
            request = AuthorizationRequest.builder().setRequestedScopes(scopesOf(scopes)).build();
        } catch (RuntimeException invalid) {
            reply.failed(failureOf(invalid));
            return;
        }
        client()
            .authorize(request)
            .addOnSuccessListener(result ->
                reply.succeeded(
                    result.hasResolution()
                        ? new Authorizing.ConsentNeeded<>(result.getPendingIntent())
                        : new Authorizing.Granted<>(authorizationOf(result))
                )
            )
            .addOnFailureListener(exception -> reply.failed(failureOf(exception)));
    }

    @Override
    public void authorizationFromConsent(Intent returned, Reply<Authorization> reply) {
        AuthorizationResult result;
        try {
            result = client().getAuthorizationResultFromIntent(returned);
        } catch (ApiException | RuntimeException exception) {
            reply.failed(failureOf(exception));
            return;
        }
        reply.succeeded(authorizationOf(result));
    }

    @Override
    public void clearToken(String accessToken, Reply<Void> reply) {
        ClearTokenRequest request;
        try {
            request = ClearTokenRequest.builder().setToken(accessToken).build();
        } catch (RuntimeException invalid) {
            reply.failed(failureOf(invalid));
            return;
        }
        client()
            .clearToken(request)
            .addOnSuccessListener(nothing -> reply.succeeded(null))
            .addOnFailureListener(exception -> reply.failed(failureOf(exception)));
    }

    @Override
    public void revokeAccess(String account, List<String> scopes, Reply<Void> reply) {
        RevokeAccessRequest request;
        try {
            request = RevokeAccessRequest.builder()
                .setAccount(new Account(account, GOOGLE_ACCOUNT_TYPE))
                .setScopes(scopesOf(scopes))
                .build();
        } catch (RuntimeException invalid) {
            reply.failed(failureOf(invalid));
            return;
        }
        client()
            .revokeAccess(request)
            .addOnSuccessListener(nothing -> reply.succeeded(null))
            .addOnFailureListener(exception -> reply.failed(failureOf(exception)));
    }
}
