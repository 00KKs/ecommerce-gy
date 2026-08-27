package com.example.e_commerce.client.pg.dto.response;

import java.time.OffsetDateTime;

public record PgPaymentResponse(
        String paymentKey,
        String orderId,
        String status,
        int amount,
        OffsetDateTime approvedAt,
        OffsetDateTime canceledAt
) {
}
