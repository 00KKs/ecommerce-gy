package com.example.e_commerce.service;

import com.example.e_commerce.client.pg.PgPaymentClient;
import com.example.e_commerce.client.pg.PgPaymentException;
import com.example.e_commerce.client.pg.PgUnknownResultException;
import com.example.e_commerce.client.pg.dto.response.PgPaymentResponse;
import com.example.e_commerce.entity.Order;
import com.example.e_commerce.entity.OrderStatus;
import com.example.e_commerce.entity.Payment;
import com.example.e_commerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// 요청 스레드에서 확정하지 못하고 PAYMENT_PENDING 으로 남은 주문을 PG 조회로 확정한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRecoveryScheduler {

    // 요청 스레드의 복구 조회(최대 수 초)와 겹치지 않도록 이 시간이 지난 주문만 본다.
    private static final Duration PENDING_GRACE = Duration.ofMinutes(1);
    // 이 시간이 지나도 PG 가 READY 면 승인 요청이 처리되지 않은 것으로 보고 만료시킨다.
    // PG 의 최대 승인 지연보다 충분히 길어야 한다.
    private static final Duration READY_EXPIRY = Duration.ofMinutes(10);

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final PgPaymentClient pgPaymentClient;
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
        String paymentKey = payment.getPaymentKey();

        PgPaymentResponse actual;
        try {
            actual = pgPaymentClient.retrieve(paymentKey);
        } catch (PgUnknownResultException e) {
            // [A] 응답 자체가 없다. PG 장애는 일시적이므로 PENDING 그대로 두고 다음 주기에 다시 묻는다.
            log.warn("복구 스케줄러: 조회 응답 없음, 다음 주기에 재시도. orderId={}, paymentKey={}", orderId, paymentKey);
            return;
        } catch (PgPaymentException e) {
            if (e.isNotFound()) {
                // PG 에 결제 건이 없다. 승인될 대상이 없으므로 대금이 빠져나갈 수 없다.
                log.info("복구 스케줄러: PG 결제 건 없음, 실패 확정. orderId={}, paymentKey={}", orderId, paymentKey);
                orderPaymentFinalizer.fail(orderId);
                return;
            }
            // 그 밖의 에러 응답. 재시도해도 같은 답이 오므로 수동 확인 대상으로 뺀다.
            log.error("복구 스케줄러: 조회 오류, 수동 확인으로 전환. orderId={}, paymentKey={}, code={}",
                    orderId, paymentKey, e.getCode(), e);
            orderPaymentFinalizer.holdForReview(orderId);
            return;
        }

        if (!String.valueOf(orderId).equals(actual.orderId()) || actual.amount() != payment.getAmount()) {
            // 다른 주문의 결제이거나 금액이 다르다. 어떤 결제가 일어났는지 모르므로 자동 확정하지 않는다.
            log.error("복구 스케줄러: 조회 불일치, 수동 확인으로 전환. paymentKey={}, 기대=(orderId={}, amount={}), 실제=(orderId={}, amount={})",
                    paymentKey, orderId, payment.getAmount(), actual.orderId(), actual.amount());
            orderPaymentFinalizer.holdForReview(orderId);
            return;
        }

        switch (actual.status()) {
            case "DONE" -> {
                log.info("복구 스케줄러: 승인 완료 확인. orderId={}, paymentKey={}", orderId, paymentKey);
                orderPaymentFinalizer.complete(orderId);
            }
            case "ABORTED" -> {
                log.info("복구 스케줄러: 승인 거절 확인. orderId={}, paymentKey={}", orderId, paymentKey);
                orderPaymentFinalizer.fail(orderId);
            }
            case "READY" -> {
                if (order.getCreatedAt().isBefore(LocalDateTime.now().minus(READY_EXPIRY))) {
                    log.info("복구 스케줄러: READY 만료, 실패 확정. orderId={}, paymentKey={}", orderId, paymentKey);
                    orderPaymentFinalizer.fail(orderId);
                }
                // 만료 전이면 아직 처리 중. 다음 주기에 다시 본다.
            }
            default -> {
                // CANCELED 등 이 흐름에서 나올 수 없는 상태.
                log.error("복구 스케줄러: 예상하지 못한 상태, 수동 확인으로 전환. orderId={}, paymentKey={}, status={}",
                        orderId, paymentKey, actual.status());
                orderPaymentFinalizer.holdForReview(orderId);
            }
        }
    }
}
