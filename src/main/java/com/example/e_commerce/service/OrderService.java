package com.example.e_commerce.service;

import com.example.e_commerce.client.pg.PgPaymentClient;
import com.example.e_commerce.client.pg.PgPaymentException;
import com.example.e_commerce.client.pg.PgUnknownResultException;
import com.example.e_commerce.client.pg.dto.response.PgPaymentResponse;
import com.example.e_commerce.dto.request.Order.OrderCreateRequest;
import com.example.e_commerce.dto.response.Address.DefaultAddressInfo;
import com.example.e_commerce.dto.response.Order.OrderDetailResponse;
import com.example.e_commerce.dto.response.OrderCreateResponse;
import com.example.e_commerce.dto.response.Sku.SkuOrderInfo;
import com.example.e_commerce.entity.*;
import com.example.e_commerce.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final int RECOVERY_MAX_ATTEMPTS = 4;
    private static final long RECOVERY_INTERVAL_MS = 1000L;

    private final AddressService addressService;
    private final SkuService skuService;
    private final StockService stockService;
    private final PaymentService paymentService;
    private final PgPaymentClient pgPaymentClient;

    private final OrderRepository orderRepository;
    private final TransactionTemplate transactionTemplate;

    public OrderCreateResponse createOrder(Long memberId, OrderCreateRequest request) {
        OrderPreparation preparation =
                transactionTemplate.execute(status -> prepareOrder(memberId, request));

        PgPaymentResponse confirmed = confirmPayment(preparation);

        return transactionTemplate.execute(status -> completeOrder(preparation.orderId(), confirmed));
    }

    /**
     * PG 결제 건 생성 → READY 저장 → 승인. 응답을 못 받으면 조회로 결과를 확정한다.
     */
    private PgPaymentResponse confirmPayment(OrderPreparation preparation) {
        try {
            PgPaymentResponse created =
                    pgPaymentClient.create(preparation.orderId(), preparation.amount());

            // 승인을 요청하기 전에 READY 를 먼저 커밋한다.
            // 이후 confirm 이 타임아웃 나도 paymentKey 가 DB 에 남아 조회로 복구할 수 있다.
            transactionTemplate.executeWithoutResult(status ->
                    paymentService.ready(preparation.orderId(), preparation.amount(), created.paymentKey()));

            return pgPaymentClient.confirm(
                    created.paymentKey(), preparation.orderId(), preparation.amount());

        } catch (PgUnknownResultException e) {
            return recover(preparation, e);

        } catch (PgPaymentException e) {
            transactionTemplate.executeWithoutResult(status -> failOrder(preparation));
            throw e;
        }
    }

    public OrderPreparation prepareOrder(Long memberId, OrderCreateRequest request) {
        DefaultAddressInfo defaultAddress = addressService.getDefaultAddress(memberId);

        Order order = new Order(
                memberId,
                defaultAddress.recipientName(),
                defaultAddress.recipientPhone(),
                defaultAddress.address(),
                defaultAddress.deliveryRequest()
        );
        orderRepository.save(order);

        SkuOrderInfo skuInfo = skuService.getSkuOrderInfo(request.getSkuId());
        stockService.decrease(skuInfo.skuId(), request.getQuantity());

        OrderItem orderItem = new OrderItem(
                order,
                skuInfo.skuId(),
                skuInfo.productName(),
                skuInfo.optionName(),
                skuInfo.price(),
                request.getQuantity()
        );
        order.addItem(orderItem);

        return new OrderPreparation(order.getId(), order.getTotalAmount(), skuInfo.skuId(), request.getQuantity());
    }

    private OrderCreateResponse completeOrder(Long orderId, PgPaymentResponse confirmed) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        paymentService.markDone(orderId);

        order.confirm();

        return new OrderCreateResponse(order, confirmed.paymentKey());
    }

    /**
     * 주문 실패 확정. 승인되지 않은 것이 확실할 때만 쓴다.
     * 재고를 되돌리고 결제를 ABORTED 로 닫는다.
     */
    private void failOrder(OrderPreparation preparation) {
        restoreStock(preparation);
        paymentService.markAbortedIfExists(preparation.orderId());
    }

    /**
     * 재고만 되돌리고 결제 상태는 건드리지 않는다. 청구 여부가 불확실할 때 쓴다.
     */
    private void restoreStock(OrderPreparation preparation) {
        stockService.restore(preparation.skuId(), preparation.quantity());
    }

    private record OrderPreparation(Long orderId, int amount, Long skuId, int quantity) {}

    /**
     * 승인 결과를 받지 못했을 때 PG 에 실제 결과를 물어 확정한다.
     * 조회는 부작용이 없으므로 실패해도 안전하게 재시도할 수 있다.
     */
    private PgPaymentResponse recover(OrderPreparation preparation, PgUnknownResultException cause) {
        if (!cause.isRecoverable()) {
            // create 단계에서 끊긴 경우. paymentKey 가 없어 물어볼 수단이 없다.
            transactionTemplate.executeWithoutResult(status -> failOrder(preparation));
            throw cause;
        }

        String paymentKey = cause.getPaymentKey().orElseThrow();

        for (int attempt = 1; attempt <= RECOVERY_MAX_ATTEMPTS; attempt++) {
            PgPaymentResponse actual;
            try {
                actual = pgPaymentClient.retrieve(paymentKey);
            } catch (PgUnknownResultException retrieveFailed) {
                log.warn("복구 조회 실패: paymentKey={}, attempt={}/{}",
                        paymentKey, attempt, RECOVERY_MAX_ATTEMPTS);
                sleep();
                continue;

            } catch (PgPaymentException retrieveRejected) {
                // PG 가 조회에 에러로 응답했다(예: NOT_FOUND_PAYMENT).
                // 재시도해도 같은 답이 오므로 루프를 끝내되, 청구 여부는 확신할 수 없다.
                log.error("복구 조회 오류: paymentKey={}, code={}",
                        paymentKey, retrieveRejected.getCode(), retrieveRejected);
                transactionTemplate.executeWithoutResult(status -> restoreStock(preparation));
                throw retrieveRejected;
            }

            if (isMismatched(actual, preparation)) {
                // 같은 키로 다시 물어도 같은 답이 오므로 재시도는 의미가 없다.
                log.error("복구 조회 불일치: paymentKey={}, 기대=(orderId={}, amount={}), 실제=(orderId={}, amount={})",
                        paymentKey, preparation.orderId(), preparation.amount(),
                        actual.orderId(), actual.amount());
                throw cause;
            }

            switch (actual.status()) {
                case "DONE" -> {
                    log.info("복구 성공: 승인 완료 확인. paymentKey={}, attempt={}", paymentKey, attempt);
                    return actual;
                }
                case "ABORTED" -> {
                    log.info("복구 성공: 승인 거절 확인. paymentKey={}", paymentKey);
                    transactionTemplate.executeWithoutResult(status -> failOrder(preparation));
                    throw new PgPaymentException("PAYMENT_REJECTED", "결제가 거절되었습니다.", cause);
                }
                default -> {
                    // READY: PG 가 아직 처리 중이다. 재고는 그대로 두고 다시 물어본다.
                    log.info("복구 대기: 아직 처리 중. paymentKey={}, attempt={}/{}",
                            paymentKey, attempt, RECOVERY_MAX_ATTEMPTS);
                    sleep();
                }
            }
        }

        log.error("복구 실패: 결과 미확정으로 종료. orderId={}, paymentKey={}",
                preparation.orderId(), paymentKey);
        throw cause;
    }


    // 조회 결과가 정말 이 주문의 결제인지 확인한다.
    private boolean isMismatched(PgPaymentResponse actual, OrderPreparation preparation) {
        return !String.valueOf(preparation.orderId()).equals(actual.orderId())
                || actual.amount() != preparation.amount();
    }

    private void sleep() {
        try {
            Thread.sleep(RECOVERY_INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("복구 대기 중 인터럽트", e);
        }
    }
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrder(Long memberId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        if (!order.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 주문만 조회할 수 있습니다.");
        }

        String paymentKey = paymentService.getPaymentKey(orderId);

        return new OrderDetailResponse(order, paymentKey);
    }
}
