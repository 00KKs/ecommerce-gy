package com.example.e_commerce.client.pg.dto.response;

public record PgErrorResponse(
        String code,
        String message
) {
}
