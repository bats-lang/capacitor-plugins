package io.github.batslang.googleauthorize;

/** How the authorization service answers a call, once. */
interface Reply<Value> {
    void succeeded(Value value);

    void failed(Failure failure);
}
