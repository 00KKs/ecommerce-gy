package com.example.e_commerce.service;

import com.example.e_commerce.client.pg.PgPaymentClient;
import com.example.e_commerce.client.pg.PgPaymentException;
import com.example.e_commerce.client.pg.PgUnknownResultException;
import com.example.e_commerce.dto.request.Order.OrderCreateRequest;
import com.example.e_commerce.dto.response.Address.DefaultAddressInfo;
import com.example.e_commerce.dto.response.Order.OrderCreateResponse;
import com.example.e_commerce.dto.response.Order.OrderDetailResponse;
import com.example.e_commerce.dto.response.Sku.SkuOrderInfo;
import com.example.e_commerce.entity.*;
import com.example.e_commerce.global.exception.IdempotencyKeyReusedException;
import com.example.e_commerce.global.exception.OrderAlreadyFailedException;
import com.example.e_commerce.global.exception.OrderAlreadyPaidException;
import com.example.e_commerce.global.exception.OrderInProgressException;
import com.example.e_commerce.global.exception.OrderPaymentFailedException;
import com.example.e_commerce.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final int RECOVERY_MAX_ATTEMPTS = 2;
    private static final long RECOVERY_INTERVAL_MS = 1000L;
    private static final String IDEMPOTENCY_KEY_CONSTRAINT = "uk_orders_member_idempotency_key";

    private final AddressService addressService;
    private final SkuService skuService;
    private final StockService stockService;
    private final PaymentService paymentService;
    private final PgPaymentClient pgPaymentClient;
    private final PaymentResolver paymentResolver;

    private final OrderRepository orderRepository;
    private final TransactionTemplate transactionTemplate;

    public OrderCreateResponse createOrder(Long memberId, String idempotencyKey, OrderCreateRequest request) {
        // 같은 키로 이미 시도한 주문이 있으면 새로 만들지 않고 그 결과로 응답한다.
        Optional<OrderCreateResponse> replayed = replayIfExists(memberId, idempotencyKey, request);
        if (replayed.isPresent()) {
            return replayed.get();
        }

        PaymentTarget target;
        try {
            target = transactionTemplate.execute(status -> prepareOrder(memberId, idempotencyKey, request));
        } catch (DataIntegrityViolationException e) {
            // 같은 키의 요청이 동시에 들어와 다른 쪽이 먼저 주문을 만들었다.
            if (!isIdempotencyKeyViolation(e)) {
                throw e;
            }
            return replayIfExists(memberId, idempotencyKey, request).orElseThrow(() -> e);
        }

        String paymentKey = confirmPayment(target);

        Order order = complete(target.orderId(), target.paymentId());
        return new OrderCreateResponse(order.getId(), order.getStatus(), target.amount(), paymentKey);
    }

    // 재결제. 결제 기한 안의 PAYMENT_FAILED 주문에 새 결제 시도를 연다.
    // 같은 멱등키로 다시 오면 그 키로 만든 결제 시도의 결과로 응답한다.
    public OrderCreateResponse repay(Long memberId, Long orderId, String idempotencyKey) {
        RepayStart start = transactionTemplate.execute(status -> startRepayment(memberId, orderId, idempotencyKey));
        if (start.replayed() != null) {
            return start.replayed();
        }

        PaymentTarget target = start.target();
        String paymentKey = confirmPayment(target);

        Order order = complete(target.orderId(), target.paymentId());
        return new OrderCreateResponse(order.getId(), order.getStatus(), target.amount(), paymentKey);
    }

    // 주문 락 안에서 멱등 재요청인지 확인하고, 아니면 PAYMENT_FAILED → PAYMENT_PENDING 으로 전이하며 결제 시도를 연다.
    // 락 안에서 확인해야 같은 키의 요청이 겹쳐도 나중 요청이 앞선 요청이 남긴 결제 시도를 보고 재생한다.
    // 이 상태 전이가 이중 결제 가드다. 동시에 들어온 다른 키의 요청은 PENDING 을 보고 거부된다.
    private RepayStart startRepayment(Long memberId, Long orderId, String idempotencyKey) {
        Order order = getOrderForUpdate(orderId);
        requireOwner(order, memberId);

        Optional<Payment> attempt = paymentService.findByOrderIdAndIdempotencyKey(orderId, idempotencyKey);
        if (attempt.isPresent()) {
            return RepayStart.ofReplay(replayRepay(order, attempt.get()));
        }

        switch (order.getStatus()) {
            case PAYMENT_FAILED -> { }
            case PAYMENT_PENDING, PAYMENT_UNKNOWN -> throw new OrderInProgressException(orderId);
            case CONFIRMED, SHIPPED -> throw new OrderAlreadyPaidException(orderId);
            case CANCELED -> throw new OrderAlreadyFailedException(orderId);
        }

        LocalDateTime now = LocalDateTime.now();
        if (order.isPaymentWindowClosed(now)) {
            // 스케줄러가 아직 취소하지 않았을 뿐 만료된 주문이다.
            throw new OrderAlreadyFailedException(orderId);
        }

        order.startRepayment(now);
        // PG 를 부르기 전에 결제 시도와 멱등키를 함께 커밋한다. PG 결제 생성이 실패해도 이 키의 기록이 남는다.
        Payment payment = paymentService.request(order, order.getTotalAmount(), idempotencyKey);
        log.info("재결제 시작: orderId={}, paymentId={}", orderId, payment.getId());
        return RepayStart.ofStarted(new PaymentTarget(orderId, payment.getId(), order.getTotalAmount()));
    }

    // 재결제 시작 결과. 같은 키의 결제 시도가 있으면 replayed, 새 시도를 열었으면 target 이 채워진다.
    private record RepayStart(OrderCreateResponse replayed, PaymentTarget target) {
        static RepayStart ofReplay(OrderCreateResponse response) {
            return new RepayStart(response, null);
        }

        static RepayStart ofStarted(PaymentTarget target) {
            return new RepayStart(null, target);
        }
    }

    private OrderCreateResponse replayRepay(Order order, Payment payment) {
        log.info("재결제 멱등 재요청: orderId={}, paymentId={}, status={}",
                order.getId(), payment.getId(), payment.getStatus());

        return switch (payment.getStatus()) {
            case DONE -> new OrderCreateResponse(
                    order.getId(), order.getStatus(), payment.getAmount(), payment.getPaymentKey());
            case REQUESTED, READY -> throw new OrderInProgressException(order.getId());
            case ABORTED -> throw new OrderPaymentFailedException(order.getId());
            case CANCELED -> throw new IllegalStateException("예상하지 못한 결제 상태입니다. status=" + payment.getStatus());
        };
    }

    private void requireOwner(Order order, Long memberId) {
        if (!order.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 주문만 결제할 수 있습니다.");
        }
    }

    // PG 결제 생성 → 결제 시도 READY 기록 → 승인. 응답을 못 받으면 조회(필요하면 재승인)로 결과를 확정한다.
    // 결제 시도 행(REQUESTED)은 호출 전에 이미 커밋돼 있다. 실패하면 그 행을 ABORTED 로 남긴다.
    private String confirmPayment(PaymentTarget target) {
        String paymentKey;
        try {
            paymentKey = pgPaymentClient.create(target.orderId(), target.amount()).paymentKey();
        } catch (PgUnknownResultException | PgPaymentException e) {
            // 결제 생성이 거절됐거나 결과를 모른다. 승인 요청을 보내지 않았으므로 대금은 빠져나가지 않았다.
            failPayment(target.orderId(), target.paymentId());
            throw new OrderPaymentFailedException(target.orderId(), e);
        }

        markReady(target, paymentKey);

        try {
            pgPaymentClient.confirm(paymentKey, target.orderId(), target.amount());
            return paymentKey;

        } catch (PgUnknownResultException e) {
            recover(target, paymentKey, e);
            return paymentKey;

        } catch (PgPaymentException e) {
            failPayment(target.orderId(), target.paymentId());
            throw new OrderPaymentFailedException(target.orderId(), e);
        }
    }

    // PG 가 발급한 paymentKey 를 결제 시도에 기록한다(REQUESTED → READY).
    // 승인을 요청하기 전에 커밋해야 타임아웃 이후에도 paymentKey 로 조회해 복구할 수 있다.
    // 주문 락 안에서 이 결제 시도가 아직 REQUESTED 인지 확인한다. 요청 스레드가 멈춘 사이 스케줄러가
    // 이 시도를 ABORTED 로 정리했다면 여기서 멈춰, 늦게 깨어난 요청이 승인을 보내지 못하게 한다.
    private void markReady(PaymentTarget target, String paymentKey) {
        transactionTemplate.executeWithoutResult(status -> {
            getOrderForUpdate(target.orderId());
            Payment payment = paymentService.getById(target.paymentId());

            if (payment.getStatus() != PaymentStatus.REQUESTED) {
                log.warn("이미 정리된 결제 시도, 승인 중단: orderId={}, paymentId={}, status={}",
                        target.orderId(), payment.getId(), payment.getStatus());
                throw new OrderInProgressException(target.orderId());
            }
            payment.markReady(paymentKey);
        });
    }

    // 주문 INSERT 가 재고 차감보다 먼저다. 같은 키의 동시 요청은 재고 락을 잡기 전에 유니크 제약에서 걸러진다.
    // 첫 결제 시도도 같은 트랜잭션에서 연다. 결제 시도 없이 PENDING 인 주문이 생기지 않는다.
    private PaymentTarget prepareOrder(Long memberId, String idempotencyKey, OrderCreateRequest request) {
        DefaultAddressInfo defaultAddress = addressService.getDefaultAddress(memberId);

        Order order = new Order(
                memberId,
                idempotencyKey,
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

        // 최초 결제 시도는 주문 멱등키로 보호되므로 결제 쪽 멱등키는 없다.
        Payment payment = paymentService.request(order, order.getTotalAmount(), null);
        return new PaymentTarget(order.getId(), payment.getId(), order.getTotalAmount());
    }

    // 결제 대상. 주문 생성과 재결제가 함께 쓴다. paymentId 는 이번에 연 결제 시도다.
    private record PaymentTarget(Long orderId, Long paymentId, int amount) {}

    // 승인 결과를 못 받았을 때. PG 에 확인(필요하면 재승인)해서 확정한다. 확정 못 하면 스케줄러에 넘긴다.
    private void recover(PaymentTarget target, String paymentKey, PgUnknownResultException cause) {
        for (int attempt = 1; attempt <= RECOVERY_MAX_ATTEMPTS; attempt++) {
            Resolution resolution = paymentResolver.resolve(target.orderId(), paymentKey, target.amount());
            log.info("복구 시도: orderId={}, paymentKey={}, attempt={}/{}, resolution={}",
                    target.orderId(), paymentKey, attempt, RECOVERY_MAX_ATTEMPTS, resolution);

            switch (resolution) {
                case CONFIRMED -> {
                    return;
                }
                case REJECTED -> {
                    failPayment(target.orderId(), target.paymentId());
                    throw new OrderPaymentFailedException(target.orderId(), cause);
                }
                case NEEDS_REVIEW -> {
                    holdForReview(target.orderId());
                    throw new OrderInProgressException(target.orderId(), cause);
                }
                case RETRY_LATER -> {
                    if (attempt < RECOVERY_MAX_ATTEMPTS) {
                        sleep();   // 마지막 시도 뒤에는 자지 않는다
                    }
                }
            }
        }

        // 끝내 응답을 못 받았다. PENDING 그대로 두면 스케줄러가 이어서 확정한다.
        log.warn("복구 보류: 스케줄러에 위임. orderId={}, paymentKey={}", target.orderId(), paymentKey);
        throw new OrderInProgressException(target.orderId(), cause);
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

        Payment payment = paymentService.findLatestByOrderId(orderId).orElse(null);

        return new OrderDetailResponse(order, payment);
    }

    // 기존 주문의 현재 상태로 응답을 재구성한다. 성공이면 값을 돌려주고, 그 외는 예외로 알린다.
    // replay 가 지연 로딩되는 items 를 읽으므로 트랜잭션 안에서 실행한다.
    private Optional<OrderCreateResponse> replayIfExists(Long memberId, String idempotencyKey, OrderCreateRequest request) {
        return transactionTemplate.execute(status ->
                orderRepository.findByMemberIdAndIdempotencyKey(memberId, idempotencyKey)
                        .map(order -> replay(order, request)));
    }

    private OrderCreateResponse replay(Order order, OrderCreateRequest request) {
        if (!order.isSameRequest(request.getSkuId(), request.getQuantity())) {
            throw new IdempotencyKeyReusedException();
        }

        log.info("멱등 재요청: orderId={}, status={}", order.getId(), order.getStatus());

        return switch (order.getStatus()) {
            case CONFIRMED, SHIPPED -> {
                String paymentKey = paymentService.findLatestByOrderId(order.getId())
                        .map(Payment::getPaymentKey)
                        .orElse(null);
                yield new OrderCreateResponse(order.getId(), order.getStatus(), order.getTotalAmount(), paymentKey);
            }
            case PAYMENT_PENDING, PAYMENT_UNKNOWN -> throw new OrderInProgressException(order.getId());
            case PAYMENT_FAILED -> throw new OrderPaymentFailedException(order.getId());
            case CANCELED -> throw new OrderAlreadyFailedException(order.getId());
        };
    }

    // 멱등키 유니크 제약 위반인지 확인한다. 다른 제약 위반까지 삼키지 않기 위해서다.
    private boolean isIdempotencyKeyViolation(DataIntegrityViolationException e) {
        return e.getCause() instanceof ConstraintViolationException cve
                && IDEMPOTENCY_KEY_CONSTRAINT.equalsIgnoreCase(cve.getConstraintName());
    }

    // 승인 확정. 주문 CONFIRMED → 해당 결제 시도 DONE.
    // 요청 스레드와 복구 스케줄러가 같은 결제 시도를 동시에 확정할 수 있다. 이미 같은 결과로 확정됐으면 그대로 돌려준다.
    public Order complete(Long orderId, Long paymentId) {
        return transactionTemplate.execute(status -> {
            Order order = getOrderForUpdate(orderId);
            Payment payment = paymentService.getById(paymentId);

            if (order.getStatus() == OrderStatus.CONFIRMED && payment.getStatus() == PaymentStatus.DONE) {
                log.info("이미 승인 확정된 결제 시도: orderId={}, paymentId={}", orderId, paymentId);
                return order;
            }

            order.confirm();
            payment.markDone();
            return order;
        });
    }

    // 결제 시도 실패 확정. 주문 PAYMENT_FAILED → 해당 결제 시도 ABORTED.
    // 재고는 그대로 둔다. 결제 기한 안에 재결제할 수 있다.
    // paymentId 가 null 이면 결제 시도 행을 먼저 저장하기 전에 만든 이전 주문이다.
    // 이미 같은 결과로 확정됐으면 아무것도 하지 않는다.
    public void failPayment(Long orderId, Long paymentId) {
        transactionTemplate.executeWithoutResult(status -> {
            Order order = getOrderForUpdate(orderId);
            Payment payment = paymentId == null ? null : paymentService.getById(paymentId);

            boolean alreadyAborted = payment == null || payment.getStatus() == PaymentStatus.ABORTED;
            if (order.getStatus() == OrderStatus.PAYMENT_FAILED && alreadyAborted) {
                log.info("이미 실패 확정된 결제 시도: orderId={}, paymentId={}", orderId, paymentId);
                return;
            }

            order.markPaymentFailed();
            if (payment != null) {
                payment.markAborted();
            }
        });
    }

    // 결제 기한이 지난 주문을 취소하고 재고를 돌려준다.
    // 재고 복원은 이 메서드에서만 일어난다. 주문 락 + expire() 가드로 동시에 호출돼도 한 번만 실행된다.
    public void expire(Long orderId) {
        transactionTemplate.executeWithoutResult(status -> {
            Order order = getOrderForUpdate(orderId);
            order.expire(LocalDateTime.now());
            order.getItems().forEach(item -> stockService.restore(item.getSkuId(), item.getQuantity()));
        });
    }

    // 재고는 건드리지 않는다. 결제가 됐는지 모르기 때문.
    public void holdForReview(Long orderId) {
        transactionTemplate.executeWithoutResult(status ->
                getOrderForUpdate(orderId).markPaymentUnknown());
    }

    private Order getOrderForUpdate(Long orderId) {
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
    }
}