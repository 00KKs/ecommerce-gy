package com.example.e_commerce.service;

import com.example.e_commerce.entity.Order;
import com.example.e_commerce.entity.OrderStatus;
import com.example.e_commerce.entity.Payment;
import com.example.e_commerce.entity.Resolution;
import com.example.e_commerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// 요청 스레드에서 확정하지 못하고 PAYMENT_PENDING 으로 남은 주문을 PG 조회(필요하면 재승인)로 확정한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRecoveryScheduler {

    // 요청 스레드의 복구 조회(최대 수 초)와 겹치지 않도록 이 시간이 지난 주문만 본다.
    private static final Duration PENDING_GRACE = Duration.ofMinutes(1);

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final PaymentResolver paymentResolver;
    private final OrderPaymentFinalizer orderPaymentFinalizer;

    @Scheduled(fixedDelay = 30_000)
    public void recover() {
        List<Order> targets = orderRepository.findTop50ByStatusAndCreatedAtBeforeOrderByIdAsc(
                OrderStatus.PAYMENT_PENDING, LocalDateTime.now().minus(PENDING_GRACE));

        for (Order order : targets) {
            try {
                recoverOne(order);
            } catch (Exception e) {
                // 한 건이 실패해도 나머지는 계속 처리한다. 다음 주기에 다시 시도된다.
                log.warn("복구 스케줄러 처리 실패: orderId={}", order.getId(), e);
            }
        }
    }

    private void recoverOne(Order order) {
        Long orderId = order.getId();
        Optional<Payment> found = paymentService.findByOrderId(orderId);

        if (found.isEmpty()) {
            log.info("복구 스케줄러: 결제 레코드 없음, 실패 확정. orderId={}", orderId);
            orderPaymentFinalizer.fail(orderId);
            return;
        }

        Payment payment = found.get();
        Resolution resolution = paymentResolver.resolve(orderId, payment.getPaymentKey(), payment.getAmount());
        log.info("복구 스케줄러: orderId={}, paymentKey={}, resolution={}", orderId, payment.getPaymentKey(), resolution);

        switch (resolution) {
            case CONFIRMED -> orderPaymentFinalizer.complete(orderId);
            case REJECTED -> orderPaymentFinalizer.fail(orderId);
            case NEEDS_REVIEW -> orderPaymentFinalizer.holdForReview(orderId);
            case RETRY_LATER -> { /* PENDING 그대로. 다음 주기에 다시 묻는다. */ }
        }
    }
}
