package com.example.e_commerce.client.pg;

public class PgPaymentException extends PgException {

    private final String code;

    public PgPaymentException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean isRejected() {
        return "PAYMENT_REJECTED".equals(code);
    }
}
