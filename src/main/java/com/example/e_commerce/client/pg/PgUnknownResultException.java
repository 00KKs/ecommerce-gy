package com.example.e_commerce.client.pg;

import java.util.Optional;

public class PgUnknownResultException extends PgException {

    private final String paymentKey;

    public PgUnknownResultException(String paymentKey, String message, Throwable cause) {
        super(message, cause);
        this.paymentKey = paymentKey;
    }

    public Optional<String> getPaymentKey() {
        return Optional.ofNullable(paymentKey);
    }

    public boolean isRecoverable() {
        return paymentKey != null;
    }
}
