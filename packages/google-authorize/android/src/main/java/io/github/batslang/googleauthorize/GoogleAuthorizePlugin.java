package io.github.batslang.googleauthorize;

import android.accounts.Account;
import android.app.Activity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.android.gms.auth.api.identity.AuthorizationClient;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.RevokeAccessRequest;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;

// Proof of concept (bats-lang/quire#321, Phase 0): Phase 2's API over
// Android's AuthorizationClient. authorize gives an access token for the
// scopes with no UI once they are granted; when consent is needed it
// rejects NEEDS_INTERACTION, or with interactive true shows Google's
// consent and rejects CANCELED when the reader backs out. revoke takes
// the grant back (revokeAccess, in play-services-auth 21.5.0).
@CapacitorPlugin(name = "GoogleAuthorize")
public class GoogleAuthorizePlugin extends Plugin {

    public static final String NEEDS_INTERACTION = "NEEDS_INTERACTION";
    public static final String CANCELED = "CANCELED";
    public static final String ALREADY_WAITING = "ALREADY_WAITING";
    public static final String NO_ACCOUNT = "NO_ACCOUNT";

    private ActivityResultLauncher<IntentSenderRequest> consentLauncher;
    // the interactive call whose consent screen is showing
    private PluginCall waitingCall;

    @Override
    public void load() {
        consentLauncher = getActivity().registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            this::consentEnded
        );
    }

    private AuthorizationClient client() {
        return Identity.getAuthorizationClient(getActivity());
    }

    private static List<Scope> scopesOf(PluginCall call) throws JSONException {
        JSArray given = call.getArray("scopes", new JSArray());
        List<Scope> scopes = new ArrayList<>();
        for (String scope : given.<String>toList()) {
            scopes.add(new Scope(scope));
        }
        return scopes;
    }

    private static void rejectFailure(PluginCall call, Exception exception) {
        String code = exception instanceof ApiException
            ? String.valueOf(((ApiException) exception).getStatusCode())
            : exception.getClass().getSimpleName();
        call.reject(String.valueOf(exception.getMessage()), code, exception);
    }

    private static void resolveToken(PluginCall call, AuthorizationResult result) {
        JSObject answer = new JSObject();
        answer.put("accessToken", result.getAccessToken());
        // POC only, for Phase 4's question: the account the grant is for,
        // when the result names it
        GoogleSignInAccount account = result.toGoogleSignInAccount();
        answer.put("email", account == null ? null : account.getEmail());
        call.resolve(answer);
    }

    @PluginMethod
    public void authorize(PluginCall call) {
        boolean interactive = Boolean.TRUE.equals(call.getBoolean("interactive", false));
        AuthorizationRequest request;
        try {
            request = AuthorizationRequest.builder().setRequestedScopes(scopesOf(call)).build();
        } catch (Exception exception) {
            rejectFailure(call, exception);
            return;
        }
        client()
            .authorize(request)
            .addOnSuccessListener(result -> {
                if (!result.hasResolution()) {
                    resolveToken(call, result);
                } else if (!interactive) {
                    call.reject("The reader must consent first", NEEDS_INTERACTION);
                } else if (waitingCall != null) {
                    call.reject("Another authorization is showing its consent", ALREADY_WAITING);
                } else {
                    waitingCall = call;
                    consentLauncher.launch(new IntentSenderRequest.Builder(result.getPendingIntent().getIntentSender()).build());
                }
            })
            .addOnFailureListener(exception -> rejectFailure(call, exception));
    }

    private void consentEnded(ActivityResult ended) {
        PluginCall call = waitingCall;
        waitingCall = null;
        if (call == null) {
            return;
        }
        if (ended.getResultCode() != Activity.RESULT_OK) {
            call.reject("The reader backed out of the consent", CANCELED);
            return;
        }
        try {
            resolveToken(call, client().getAuthorizationResultFromIntent(ended.getData()));
        } catch (ApiException exception) {
            rejectFailure(call, exception);
        }
    }

    @PluginMethod
    public void revoke(PluginCall call) {
        List<Scope> scopes;
        try {
            scopes = scopesOf(call);
        } catch (Exception exception) {
            rejectFailure(call, exception);
            return;
        }
        // the account the grant is for: a silent authorize names it
        client()
            .authorize(AuthorizationRequest.builder().setRequestedScopes(scopes).build())
            .addOnSuccessListener(result -> {
                GoogleSignInAccount signedIn = result.hasResolution() ? null : result.toGoogleSignInAccount();
                Account account = signedIn == null ? null : signedIn.getAccount();
                if (account == null) {
                    call.reject("No account holds the grant", NO_ACCOUNT);
                    return;
                }
                client()
                    .revokeAccess(RevokeAccessRequest.builder().setAccount(account).setScopes(scopes).build())
                    .addOnSuccessListener(nothing -> call.resolve())
                    .addOnFailureListener(exception -> rejectFailure(call, exception));
            })
            .addOnFailureListener(exception -> rejectFailure(call, exception));
    }
}
