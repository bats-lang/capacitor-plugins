package io.github.batslang.googleauthorize;

/** How a call of the plugin answers its caller, once. */
interface Answer {
    /** Resolves with the authorization. */
    void authorized(Authorization authorization);

    /** Resolves with no authorization: consent is needed, and nothing was shown. */
    void notAuthorized();

    /** Resolves with nothing. */
    void done();

    /** Rejects. */
    void failed(Failure failure);
}
