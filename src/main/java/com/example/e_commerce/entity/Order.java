package com.example.e_commerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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

    public Order(Long memberId, String idempotencyKey, String recipientName, String recipientPhone,
                 String address, String deliveryRequest) {
        this.memberId = memberId;
        this.idempotencyKey = idempotencyKey;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.address = address;
        this.deliveryRequest = deliveryRequest;
        this.status = OrderStatus.PAYMENT_PENDING;
    }

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

    public void cancel() {
        requirePaymentPending();
        this.status = OrderStatus.CANCELED;
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
    public void ship() {
        if (this.status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("결제 완료된 주문만 배송할 수 있습니다. status=" + status);
        }
        this.status = OrderStatus.SHIPPED;
    }
}
