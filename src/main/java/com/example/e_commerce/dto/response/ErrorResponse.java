package com.example.e_commerce.dto.response;

import lombok.Getter;

@Getter
public class ErrorResponse {
    private final String message;

    public ErrorResponse(String message) {
        this.message = message;
    }
}
