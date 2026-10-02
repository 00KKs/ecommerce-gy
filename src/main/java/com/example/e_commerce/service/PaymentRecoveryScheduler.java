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
            // READY 저장 전에 끊겼다. 승인 요청을 보낸 적이 없으므로 대금이 빠져나갈 수 없다.
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
            log.warn("복구 스케줄러: 조회 응답 없음, 다음 주기에 재시도. orderId={}, paymentKey={}", orderId, paymentKey);
            return;
        } catch (PgPaymentException e) {
            // 우리가 발급받은 paymentKey 인데 PG 가 에러로 답했다. 승인 여부를 알 수 없으므로 수동 확인 대상으로 둔다.
            log.error("복구 스케줄러: 조회 오류, 수동 확인 필요. orderId={}, paymentKey={}, code={}",
                    orderId, paymentKey, e.getCode(), e);
            return;
        }

        if (!String.valueOf(orderId).equals(actual.orderId()) || actual.amount() != payment.getAmount()) {
            log.error("복구 스케줄러: 조회 불일치, 수동 확인 필요. paymentKey={}, 기대=(orderId={}, amount={}), 실제=(orderId={}, amount={})",
                    paymentKey, orderId, payment.getAmount(), actual.orderId(), actual.amount());
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
            }
            default -> log.error("복구 스케줄러: 예상하지 못한 상태, 수동 확인 필요. orderId={}, paymentKey={}, status={}",
                    orderId, paymentKey, actual.status());
        }
    }
}
