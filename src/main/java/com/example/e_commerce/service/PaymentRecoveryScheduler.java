package com.example.e_commerce.service;

import com.example.e_commerce.entity.Order;
import com.example.e_commerce.entity.OrderStatus;
import com.example.e_commerce.entity.Payment;
import com.example.e_commerce.entity.PaymentStatus;
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

// 1) 요청 스레드에서 확정하지 못하고 PAYMENT_PENDING 으로 남은 주문을 PG 조회(필요하면 재승인)로 확정한다.
// 2) 결제 기한이 지난 PAYMENT_FAILED 주문을 취소하고 재고를 돌려준다.
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRecoveryScheduler {

    // 요청 스레드의 복구 조회(최대 수 초)와 겹치지 않도록 이 시간이 지난 주문만 본다.
    private static final Duration PENDING_GRACE = Duration.ofMinutes(1);

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final PaymentResolver paymentResolver;
    private final OrderService orderService;

    @Scheduled(fixedDelay = 30_000)
    public void recover() {
        List<Order> targets = orderRepository.findTop50ByStatusAndPaymentStartedAtBeforeOrderByIdAsc(
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

    @Scheduled(fixedDelay = 60_000)
    public void expireUnpaid() {
        List<Order> targets = orderRepository.findTop50ByStatusAndCreatedAtBeforeOrderByIdAsc(
                OrderStatus.PAYMENT_FAILED, LocalDateTime.now().minus(Order.PAYMENT_WINDOW));

        for (Order order : targets) {
            try {
                orderService.expire(order.getId());
            } catch (Exception e) {
                // 조회 이후 재결제가 시작돼 상태가 바뀌었을 수 있다. 가드에 걸린 것이므로 건너뛴다.
                log.warn("만료 처리 실패: orderId={}", order.getId(), e);
            }
        }
    }

    private void recoverOne(Order order) {
        Long orderId = order.getId();
        Optional<Payment> found = paymentService.findLatestByOrderId(orderId);

        if (found.isEmpty()) {
            // 결제 시도 행을 먼저 저장하기 전에 만든 이전 주문이다.
            log.info("복구 스케줄러: 결제 시도 없음, 결제 실패 확정. orderId={}", orderId);
            orderService.failPayment(orderId, null);
            return;
        }

        Payment payment = found.get();
        switch (payment.getStatus()) {
            case REQUESTED -> {
                // PG 결제 생성 중에 끊겼다. 승인 요청 전이므로 대금은 빠져나가지 않았다.
                // 이 시도를 ABORTED 로 정리하면, 늦게 깨어난 요청 스레드는 markReady 에서 멈춘다.
                log.info("복구 스케줄러: 결제 생성 중 끊긴 시도, 결제 실패 확정. orderId={}, paymentId={}", orderId, payment.getId());
                orderService.failPayment(orderId, payment.getId());
                return;
            }
            case READY -> { /* 아래에서 PG 에 묻는다 */ }
            default -> {
                // PENDING 주문의 최신 시도가 DONE/ABORTED 인 것은 일어나지 않아야 하는 상태다. 사람이 본다.
                log.error("주문과 결제 시도 상태 불일치: orderId={}, paymentId={}, status={}",
                        orderId, payment.getId(), payment.getStatus());
                orderService.holdForReview(orderId);
                return;
            }
        }

        Resolution resolution = paymentResolver.resolve(orderId, payment.getPaymentKey(), payment.getAmount());
        log.info("복구 스케줄러: orderId={}, paymentKey={}, resolution={}", orderId, payment.getPaymentKey(), resolution);

        switch (resolution) {
            case CONFIRMED -> orderService.complete(orderId, payment.getId());
            case REJECTED -> orderService.failPayment(orderId, payment.getId());
            case NEEDS_REVIEW -> orderService.holdForReview(orderId);
            case RETRY_LATER -> { /* PENDING 그대로. 다음 주기에 다시 묻는다. */ }
        }
    }
}
