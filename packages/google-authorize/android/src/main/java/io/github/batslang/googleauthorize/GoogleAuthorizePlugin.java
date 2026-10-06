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

    /** The call's scopes as given: the array's items, whatever they are, or null when there is no array. */
    private static List<Object> scopesOf(PluginCall call) {
        JSArray given = call.getArray("scopes");
        if (given == null) {
            return null;
        }
        List<Object> items = new ArrayList<>();
        for (int i = 0; i < given.length(); i++) {
            items.add(given.opt(i));
        }
        return items;
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

    // Each method only reads its options; GoogleAuthorize checks them (INVALID_OPTIONS) before Play services is called

    @PluginMethod
    public void authorizationForScopes(PluginCall call) {
        authorize.authorizationForScopes(scopesOf(call), answerOf(call));
    }

    @PluginMethod
    public void authorizeScopes(PluginCall call) {
        authorize.authorizeScopes(scopesOf(call), answerOf(call));
    }

    @PluginMethod
    public void clearAuthorizationToken(PluginCall call) {
        authorize.clearAuthorizationToken(call.getString("accessToken"), answerOf(call));
    }

    @PluginMethod
    public void revokeAccess(PluginCall call) {
        authorize.revokeAccess(call.getString("account"), scopesOf(call), answerOf(call));
    }
}
