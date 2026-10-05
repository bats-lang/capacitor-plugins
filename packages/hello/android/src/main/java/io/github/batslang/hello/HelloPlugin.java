package io.github.batslang.hello;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

// Proof of concept only (bats-lang/quire#321, Phase 0): echo gives back
// the value it is given, to show that a plugin installed from a
// subfolder of this repository is registered and callable
@CapacitorPlugin(name = "Hello")
public class HelloPlugin extends Plugin {

    @PluginMethod
    public void echo(PluginCall call) {
        JSObject answer = new JSObject();
        answer.put("value", call.getString("value"));
        call.resolve(answer);
    }
}
