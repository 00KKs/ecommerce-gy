package com.example.e_commerce.service;

import com.example.e_commerce.client.pg.PgPaymentClient;
import com.example.e_commerce.client.pg.PgPaymentException;
import com.example.e_commerce.client.pg.PgUnknownResultException;
import com.example.e_commerce.client.pg.dto.response.PgPaymentResponse;
import com.example.e_commerce.entity.Resolution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// PG 에 결제 결과를 물어 판단만 한다. DB 는 건드리지 않는다.
// HTTP 호출 동안 커넥션을 잡지 않도록 트랜잭션을 걸지 않는다.
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentResolver {

    private final PgPaymentClient pgPaymentClient;

    public Resolution resolve(Long orderId, String paymentKey, int amount) {
        PgPaymentResponse actual;
        try {
            actual = pgPaymentClient.retrieve(paymentKey);
        } catch (PgUnknownResultException e) {
            return Resolution.RETRY_LATER;
        } catch (PgPaymentException e) {
            if (e.isNotFound()) {
                // PG 에 결제 건이 없다. 승인될 대상이 없으므로 대금이 빠져나갈 수 없다.
                return Resolution.REJECTED;
            }
            log.error("결제 조회 오류: orderId={}, paymentKey={}, code={}", orderId, paymentKey, e.getCode(), e);
            return Resolution.NEEDS_REVIEW;
        }

        if (!String.valueOf(orderId).equals(actual.orderId()) || actual.amount() != amount) {
            log.error("결제 조회 불일치: paymentKey={}, 기대=(orderId={}, amount={}), 실제=(orderId={}, amount={})",
                    paymentKey, orderId, amount, actual.orderId(), actual.amount());
            return Resolution.NEEDS_REVIEW;
        }

        return switch (actual.status()) {
            case "DONE" -> Resolution.CONFIRMED;
            case "ABORTED" -> Resolution.REJECTED;
            case "READY" -> reconfirm(orderId, paymentKey, amount);
            default -> {
                log.error("예상하지 못한 결제 상태: orderId={}, paymentKey={}, status={}", orderId, paymentKey, actual.status());
                yield Resolution.NEEDS_REVIEW;
            }
        };
    }

    // READY = 승인 요청이 아직 처리되지 않았거나 PG 에 닿지 않았다.
    // 같은 멱등키로 다시 승인을 요청하면 PG 가 최초 결과를 재생하거나 지금 처리하므로 이중 결제 없이 확정 응답을 받는다.
    private Resolution reconfirm(Long orderId, String paymentKey, int amount) {
        try {
            pgPaymentClient.confirm(paymentKey, orderId, amount);
            log.info("재승인 성공: orderId={}, paymentKey={}", orderId, paymentKey);
            return Resolution.CONFIRMED;
        } catch (PgUnknownResultException e) {
            return Resolution.RETRY_LATER;
        } catch (PgPaymentException e) {
            if (e.isRejected()) {
                return Resolution.REJECTED;
            }
            // 400 "이미 처리된 결제입니다" 등. 승인 여부를 단정할 수 없다.
            log.error("재승인 오류: orderId={}, paymentKey={}, code={}", orderId, paymentKey, e.getCode(), e);
            return Resolution.NEEDS_REVIEW;
        }
    }
}
