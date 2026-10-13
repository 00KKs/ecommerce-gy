package com.example.e_commerce.service;

import com.example.e_commerce.entity.Order;
import com.example.e_commerce.entity.Payment;
import com.example.e_commerce.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    // 결제 시도를 연다. 주문 상태 전이와 같은 트랜잭션에서 호출되어 PG 호출 전에 커밋된다.
    public Payment request(Order order, int amount, String idempotencyKey) {
        return paymentRepository.save(Payment.request(order, amount, idempotencyKey));
    }

    public Payment getById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalStateException("결제 시도를 찾을 수 없습니다. paymentId=" + paymentId));
    }

    // 주문의 최신 결제 시도.
    @Transactional(readOnly = true)
    public Optional<Payment> findLatestByOrderId(Long orderId) {
        return paymentRepository.findTopByOrderIdOrderByIdDesc(orderId);
    }

    // 재결제 멱등키로 만든 결제 시도.
    public Optional<Payment> findByOrderIdAndIdempotencyKey(Long orderId, String idempotencyKey) {
        return paymentRepository.findByOrderIdAndIdempotencyKey(orderId, idempotencyKey);
    }
}