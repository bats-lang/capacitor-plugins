package io.github.batslang.googleauthorize;

/** Why a call failed: a code (the platform's, or one of the plugin's own) and a message. */
final class Failure {

    final String code;
    final String message;

    Failure(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
