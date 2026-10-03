package com.example.e_commerce.service;

import com.example.e_commerce.client.pg.PgPaymentClient;
import com.example.e_commerce.client.pg.PgPaymentException;
import com.example.e_commerce.client.pg.PgUnknownResultException;
import com.example.e_commerce.client.pg.dto.response.PgPaymentResponse;
import com.example.e_commerce.dto.request.Order.OrderCreateRequest;
import com.example.e_commerce.dto.response.Address.DefaultAddressInfo;
import com.example.e_commerce.dto.response.Order.OrderCreateResponse;
import com.example.e_commerce.dto.response.Order.OrderDetailResponse;
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

    private static final int RECOVERY_MAX_ATTEMPTS = 2;
    private static final long RECOVERY_INTERVAL_MS = 1000L;

    private final AddressService addressService;
    private final SkuService skuService;
    private final StockService stockService;
    private final PaymentService paymentService;
    private final PgPaymentClient pgPaymentClient;
    private final OrderPaymentFinalizer orderPaymentFinalizer;
    private final PaymentResolver paymentResolver;

    private final OrderRepository orderRepository;
    private final TransactionTemplate transactionTemplate;

    public OrderCreateResponse createOrder(Long memberId, OrderCreateRequest request) {
        OrderPreparation preparation =
                transactionTemplate.execute(status -> prepareOrder(memberId, request));

        String paymentKey = confirmPayment(preparation);

        Order order = orderPaymentFinalizer.complete(preparation.orderId());
        return new OrderCreateResponse(order, paymentKey);
    }


    // PG 결제 건 생성 → READY 저장 → 승인. 응답을 못 받으면 조회(필요하면 재승인)로 결과를 확정한다.
    private String confirmPayment(OrderPreparation preparation) {
        try {
            PgPaymentResponse created =
                    pgPaymentClient.create(preparation.orderId(), preparation.amount());

            // 승인을 요청하기 전에 READY 를 먼저 커밋한다.
            // 이후 confirm 이 타임아웃 나도 paymentKey 가 DB 에 남아 조회로 복구할 수 있다.
            transactionTemplate.executeWithoutResult(status ->
                    paymentService.ready(preparation.orderId(), preparation.amount(), created.paymentKey()));

            return pgPaymentClient.confirm(
                    created.paymentKey(), preparation.orderId(), preparation.amount())
                    .paymentKey();

        } catch (PgUnknownResultException e) {
            return recover(preparation, e);

        } catch (PgPaymentException e) {
            orderPaymentFinalizer.fail(preparation.orderId());
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

        return new OrderPreparation(order.getId(), order.getTotalAmount());
    }

    private record OrderPreparation(Long orderId, int amount) {}

    // 승인 결과를 못 받았을 때. PG 에 확인(필요하면 재승인)해서 확정한다. 확정 못 하면 스케줄러에 넘긴다.
    private String recover(OrderPreparation preparation, PgUnknownResultException cause) {
        if (!cause.isRecoverable()) {
            // create 단계에서 끊긴 경우. paymentKey 가 없어 물어볼 수단이 없다.
            orderPaymentFinalizer.fail(preparation.orderId());
            throw cause;
        }

        String paymentKey = cause.getPaymentKey().orElseThrow();

        for (int attempt = 1; attempt <= RECOVERY_MAX_ATTEMPTS; attempt++) {
            Resolution resolution = paymentResolver.resolve(preparation.orderId(), paymentKey, preparation.amount());
            log.info("복구 시도: orderId={}, paymentKey={}, attempt={}/{}, resolution={}",
                    preparation.orderId(), paymentKey, attempt, RECOVERY_MAX_ATTEMPTS, resolution);

            switch (resolution) {
                case CONFIRMED -> {
                    return paymentKey;
                }
                case REJECTED -> {
                    orderPaymentFinalizer.fail(preparation.orderId());
                    throw new PgPaymentException("PAYMENT_REJECTED", "결제가 거절되었습니다.", cause);
                }
                case NEEDS_REVIEW -> {
                    orderPaymentFinalizer.holdForReview(preparation.orderId());
                    throw cause;
                }
                case RETRY_LATER -> {
                    if (attempt < RECOVERY_MAX_ATTEMPTS) {
                        sleep();   // 마지막 시도 뒤에는 자지 않는다
                    }
                }
            }
        }

        // 끝내 응답을 못 받았다. PENDING 그대로 두면 스케줄러가 이어서 확정한다.
        log.warn("복구 보류: 스케줄러에 위임. orderId={}, paymentKey={}", preparation.orderId(), paymentKey);
        throw cause;
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

        Payment payment = paymentService.findByOrderId(orderId).orElse(null);

        return new OrderDetailResponse(order, payment);
    }
}
