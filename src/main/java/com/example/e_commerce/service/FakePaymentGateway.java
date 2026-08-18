package com.example.e_commerce.service;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FakePaymentGateway {

    public PaymentResult requestPayment(Long orderId, int amount) {
        String transactionId = UUID.randomUUID().toString();
        return new PaymentResult(true, transactionId);
    }

    public record PaymentResult(boolean success, String transactionId) {

    }
}
