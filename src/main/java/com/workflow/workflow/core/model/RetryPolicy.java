package com.workflow.workflow.core.model;


import lombok.Getter;

@Getter
public class RetryPolicy {

    public static final RetryPolicy NO_RETRY = new RetryPolicy(1, 0);
    public static final RetryPolicy RETRY_ONCE = new RetryPolicy(2, 1000);
    public static final RetryPolicy RETRY_THRICE = new RetryPolicy(4, 2000);

    private final int maxAttempts;
    private final long backoffMs;

    public RetryPolicy(int maxAttempts, long backoffMs) {
        if (maxAttempts < 1)
            throw new IllegalArgumentException("maxAttempts must be >= 1, got: " + maxAttempts);
        if (backoffMs < 0)
            throw new IllegalArgumentException("backoffMs must be >= 0, got: " + backoffMs);
        this.maxAttempts = maxAttempts;
        this.backoffMs = backoffMs;
    }


    @Override
    public String toString() {
        return "RetryPolicy{maxAttempts=" + maxAttempts + ", backoffMs=" + backoffMs + "}";
    }
}