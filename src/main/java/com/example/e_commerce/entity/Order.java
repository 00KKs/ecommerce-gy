package com.example.e_commerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_orders_member_idempotency_key",
                columnNames = {"member_id", "idempotency_key"}))
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "member_id")
    private Long memberId;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @CreatedDate
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private String recipientPhone;

    @Column(nullable = false)
    private String address;

    private String deliveryRequest;

    @Column(nullable = false)
    private LocalDateTime paymentStartedAt;

    public Order(Long memberId, String idempotencyKey, String recipientName, String recipientPhone,
                 String address, String deliveryRequest) {
        this.memberId = memberId;
        this.idempotencyKey = idempotencyKey;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.address = address;
        this.deliveryRequest = deliveryRequest;
        this.status = OrderStatus.PAYMENT_PENDING;
        this.paymentStartedAt = LocalDateTime.now();
    }

    // 결제 기한. 주문 생성 후 이 시간 안에서만 결제 시도를 시작할 수 있다.
    public static final Duration PAYMENT_WINDOW = Duration.ofMinutes(15);

    public void addItem(OrderItem item) {
        items.add(item);
    }

    public int getTotalAmount() {
        return items.stream().mapToInt(OrderItem::getTotalPrice).sum();
    }

    public void confirm() {
        requirePaymentPending();
        this.status = OrderStatus.CONFIRMED;
    }

    public void markPaymentUnknown() {
        requirePaymentPending();
        this.status = OrderStatus.PAYMENT_UNKNOWN;
    }

    private void requirePaymentPending() {
        if (this.status != OrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("결제 대기 중인 주문이 아닙니다. status=" + status);
        }
    }

    public boolean isSameRequest(Long skuId, int quantity) {
        OrderItem item = items.get(0);
        return item.getSkuId().equals(skuId) && item.getQuantity() == quantity;
    }

    public void markPaymentFailed() {
        requirePaymentPending();
        this.status = OrderStatus.PAYMENT_FAILED;
    }

    // 결제 기한이 지나도록 결제되지 않은 주문을 취소한다.
    public void expire(LocalDateTime now) {
        if (this.status != OrderStatus.PAYMENT_FAILED) {
            throw new IllegalStateException("결제 실패 상태의 주문만 만료할 수 있습니다. status=" + status);
        }
        if (!isPaymentWindowClosed(now)) {
            throw new IllegalStateException("아직 결제 기한이 남은 주문입니다.");
        }
        this.status = OrderStatus.CANCELED;
    }

    public void startRepayment(LocalDateTime now) {
        if (this.status != OrderStatus.PAYMENT_FAILED) {
            throw new IllegalStateException("결제 실패 상태의 주문만 재결제할 수 있습니다. status=" + status);
        }
        if (isPaymentWindowClosed(now)) {
            throw new IllegalStateException("결제 기한이 지난 주문입니다.");
        }
        this.status = OrderStatus.PAYMENT_PENDING;
        this.paymentStartedAt = now;
    }

    public boolean isPaymentWindowClosed(LocalDateTime now) {
        return !now.isBefore(createdAt.plus(PAYMENT_WINDOW));
    }

    public void ship() {
        if (this.status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("결제 완료된 주문만 배송할 수 있습니다. status=" + status);
        }
        this.status = OrderStatus.SHIPPED;
    }
}
