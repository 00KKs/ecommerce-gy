package com.example.e_commerce.client.pg.dto.request;

public record PgCreatePaymentRequest(
        String orderId,
        Integer amount
) {
}
