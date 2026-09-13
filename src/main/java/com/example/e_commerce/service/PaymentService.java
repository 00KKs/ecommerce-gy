package com.example.e_commerce.service;

import com.example.e_commerce.entity.Order;
import com.example.e_commerce.entity.Payment;
import com.example.e_commerce.entity.PaymentStatus;
import com.example.e_commerce.repository.OrderRepository;
import com.example.e_commerce.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    /**
     * PG 결제 건 생성 직후 READY 로 남긴다.
     * 승인 결과를 받기 전에 커밋되어야 타임아웃 이후에도 paymentKey 가 살아남는다.
     */
    public Payment ready(Long orderId, int amount, String paymentKey) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        return paymentRepository.save(Payment.ready(order, amount, paymentKey));
    }

    public void markDone(Long orderId) {
        paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("결제 정보를 찾을 수 없습니다."))
                .markDone();
    }

    /**
     * create 단계에서 실패했다면 Payment 레코드 자체가 없다.
     * 그때는 남길 결제가 없으므로 아무것도 하지 않는다.
     */
    public void markAbortedIfExists(Long orderId) {
        paymentRepository.findByOrderId(orderId).ifPresent(Payment::markAborted);
    }

    public String getPaymentKey(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("결제 정보를 찾을 수 없습니다."))
                .getPaymentKey();
    }
}