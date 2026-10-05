package io.github.batslang.googleauthorize;

/** Shows Google's consent screen; its result comes back to GoogleAuthorize.consentEnded. */
interface ConsentScreen<Consent> {
    void show(Consent consent);
}
