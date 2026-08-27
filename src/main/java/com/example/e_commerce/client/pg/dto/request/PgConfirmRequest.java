package com.example.e_commerce.client.pg.dto.request;

public record PgConfirmRequest(
        String paymentKey,
        String orderId,
        Integer amount
) {
}
