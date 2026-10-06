package io.github.batslang.googleauthorize;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;

/**
 * Google authorization with no sign-in, over Play services' AuthorizationClient (bats-lang/quire#321): the calls of
 * GoogleAuthorize, each answered as definitions.ts says.
 */
@CapacitorPlugin(name = "GoogleAuthorize")
public class GoogleAuthorizePlugin extends Plugin {

    /** An option is missing or is not what the method takes. */
    static final String INVALID_OPTIONS = GoogleAuthorize.INVALID_OPTIONS;

    private GoogleAuthorize<PendingIntent, Intent> authorize;

    @Override
    public void load() {
        // Registered while the activity is created, as an ActivityResultLauncher must be
        ActivityResultLauncher<IntentSenderRequest> consentLauncher = getActivity().registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            ended -> authorize.consentEnded(ended.getResultCode() == Activity.RESULT_OK, ended.getData())
        );
        authorize = new GoogleAuthorize<>(new PlayAuthorizationService(getActivity()), consent ->
            consentLauncher.launch(new IntentSenderRequest.Builder(consent.getIntentSender()).build())
        );
    }

    /** The call's scopes: an array of strings, else null (GoogleAuthorize checks that there is one, none blank). */
    private static List<String> scopesOf(PluginCall call) {
        JSArray given = call.getArray("scopes");
        if (given == null) {
            return null;
        }
        List<String> scopes = new ArrayList<>();
        for (int i = 0; i < given.length(); i++) {
            Object scope = given.opt(i);
            if (!(scope instanceof String)) {
                return null;
            }
            scopes.add((String) scope);
        }
        return scopes;
    }

    private static Answer answerOf(PluginCall call) {
        return new Answer() {
            @Override
            public void authorized(Authorization authorization) {
                JSObject given = new JSObject();
                given.put("accessToken", authorization.accessToken);
                given.put("grantedScopes", new JSONArray(authorization.grantedScopes));
                given.put("account", authorization.account == null ? JSObject.NULL : authorization.account);
                JSObject answer = new JSObject();
                answer.put("authorization", given);
                call.resolve(answer);
            }

            @Override
            public void notAuthorized() {
                JSObject answer = new JSObject();
                answer.put("authorization", JSObject.NULL);
                call.resolve(answer);
            }

            @Override
            public void done() {
                call.resolve();
            }

            @Override
            public void failed(Failure failure) {
                call.reject(failure.message, failure.code);
            }
        };
    }

    @PluginMethod
    public void authorizationForScopes(PluginCall call) {
        List<String> scopes = scopesOf(call);
        if (scopes == null) {
            call.reject(GoogleAuthorize.INVALID_SCOPES, INVALID_OPTIONS);
            return;
        }
        authorize.authorizationForScopes(scopes, answerOf(call));
    }

    @PluginMethod
    public void authorizeScopes(PluginCall call) {
        List<String> scopes = scopesOf(call);
        if (scopes == null) {
            call.reject(GoogleAuthorize.INVALID_SCOPES, INVALID_OPTIONS);
            return;
        }
        authorize.authorizeScopes(scopes, answerOf(call));
    }

    @PluginMethod
    public void clearAuthorizationToken(PluginCall call) {
        String accessToken = call.getString("accessToken");
        if (accessToken == null || accessToken.isEmpty()) {
            call.reject("accessToken must be a non-empty string", INVALID_OPTIONS);
            return;
        }
        authorize.clearAuthorizationToken(accessToken, answerOf(call));
    }

    @PluginMethod
    public void revokeAccess(PluginCall call) {
        String account = call.getString("account");
        List<String> scopes = scopesOf(call);
        if (account == null || account.isEmpty() || scopes == null) {
            call.reject("account must be a non-empty string, and " + GoogleAuthorize.INVALID_SCOPES, INVALID_OPTIONS);
            return;
        }
        authorize.revokeAccess(account, scopes, answerOf(call));
    }
}
