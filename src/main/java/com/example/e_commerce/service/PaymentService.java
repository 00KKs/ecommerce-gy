package com.example.e_commerce.service;

import com.example.e_commerce.entity.Order;
import com.example.e_commerce.entity.Payment;
import com.example.e_commerce.entity.PaymentStatus;
import com.example.e_commerce.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public Payment confirmSuccess(Order order, int amount, String paymentKey) {
        Payment payment = new Payment(order, amount, PaymentStatus.DONE, paymentKey);
        return paymentRepository.save(payment);
    }

    public Payment confirmFailure(Order order, int amount, String paymentKey) {
        Payment payment = new Payment(order, amount, PaymentStatus.ABORTED, paymentKey);
        return paymentRepository.save(payment);
    }

    public Payment getPayment(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("결제 정보를 찾을 수 없습니다."));
    }
}
