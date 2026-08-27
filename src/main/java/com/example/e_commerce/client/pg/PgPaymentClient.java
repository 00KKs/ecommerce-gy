package com.example.e_commerce.client.pg;

import com.example.e_commerce.client.pg.dto.request.PgConfirmRequest;
import com.example.e_commerce.client.pg.dto.request.PgCreatePaymentRequest;
import com.example.e_commerce.client.pg.dto.response.PgErrorResponse;
import com.example.e_commerce.client.pg.dto.response.PgPaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

@Component
public class PgPaymentClient {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final RestClient restClient;

    public PgPaymentClient(@Value("${pg.base-url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public PgPaymentResponse create(Long orderId, int amount) {
        try {
            return restClient.post()
                    .uri("/v1/payments")
                    .body(new PgCreatePaymentRequest(String.valueOf(orderId), amount))
                    .retrieve()
                    .body(PgPaymentResponse.class);
        } catch (HttpStatusCodeException e) {
            throw toPaymentException(e);
        }
    }

    public PgPaymentResponse confirm(String paymentKey, Long orderId, int amount) {
        try {
            return restClient.post()
                    .uri("/v1/payments/confirm")
                    .header(IDEMPOTENCY_HEADER, "confirm:" + paymentKey)
                    .body(new PgConfirmRequest(paymentKey, String.valueOf(orderId), amount))
                    .retrieve()
                    .body(PgPaymentResponse.class);
        } catch (HttpStatusCodeException e) {
            throw toPaymentException(e);
        }
    }

    private PgPaymentException toPaymentException(HttpStatusCodeException e) {
        PgErrorResponse error = e.getResponseBodyAs(PgErrorResponse.class);
        String code = error != null ? error.code() : "UNKNOWN";
        String message = error != null ? error.message() : e.getMessage();
        return new PgPaymentException(code, message, e);
    }
}
