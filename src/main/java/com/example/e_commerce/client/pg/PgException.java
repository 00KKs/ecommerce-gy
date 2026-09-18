package com.example.e_commerce.client.pg;

public abstract class PgException extends RuntimeException {

    protected PgException(String message, Throwable cause) {
        super(message, cause);
    }
}
