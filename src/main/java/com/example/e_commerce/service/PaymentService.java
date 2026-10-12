package com.example.e_commerce.service;

import com.example.e_commerce.entity.Order;
import com.example.e_commerce.entity.Payment;
import com.example.e_commerce.entity.PaymentStatus;
import com.example.e_commerce.repository.OrderRepository;
import com.example.e_commerce.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;


    // 승인 결과를 받기 전에 커밋되어야 타임아웃 이후에도 paymentKey 가 살아남는다.
    public Payment ready(Long orderId, int amount, String paymentKey, String idempotencyKey) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        return paymentRepository.save(Payment.ready(order, amount, paymentKey, idempotencyKey));
    }

    public void markDone(String paymentKey) {
        getByPaymentKey(paymentKey).markDone();
    }

    public void markAborted(String paymentKey) {
        getByPaymentKey(paymentKey).markAborted();
    }

    // 주문의 최신 결제 시도. created 단계에서 실패시 payment 없을 수 있다.
    @Transactional(readOnly = true)
    public Optional<Payment> findLatestByOrderId(Long orderId) {
        return paymentRepository.findTopByOrderIdOrderByIdDesc(orderId);
    }

    private Payment getByPaymentKey(String paymentKey) {
        return paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new IllegalStateException("결제 정보를 찾을 수 없습니다. paymentKey=" + paymentKey));
    }

    // 재결제 멱등키로 만든 결제 시도.
    public Optional<Payment> findByOrderIdAndIdempotencyKey(Long orderId, String idempotencyKey) {
        return paymentRepository.findByOrderIdAndIdempotencyKey(orderId, idempotencyKey);
    }
}