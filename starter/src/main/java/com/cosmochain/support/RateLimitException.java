package com.cosmochain.support;

import java.io.IOException;

/**
 * Exception representing an API rate-limit response with retry timing.
 */
public class RateLimitException extends IOException {
    private final long retryAfterSeconds;

    /**
     * Creates a rate-limit exception.
     *
     * @param message exception message.
     * @param retryAfterSeconds suggested retry delay in seconds.
     */
    public RateLimitException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * Returns suggested retry delay in seconds.
     *
     * @return retry delay.
     */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
