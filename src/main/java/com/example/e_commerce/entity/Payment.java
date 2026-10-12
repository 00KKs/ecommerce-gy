package com.example.e_commerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(indexes = @Index(name = "idx_payment_order_id", columnList = "order_id"),
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payment_order_idempotency_key",
                columnNames = {"order_id", "idempotency_key"}))
@Getter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false, unique = true)
    private String paymentKey;

    // 재결제 요청의 멱등키. 최초 주문의 결제 시도는 주문 멱등키로 보호되므로 null.
    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    private Payment(Order order, int amount, PaymentStatus status, String paymentKey, String idempotencyKey) {
        this.order = order;
        this.amount = amount;
        this.status = status;
        this.paymentKey = paymentKey;
        this.idempotencyKey = idempotencyKey;
    }

    public static Payment ready(Order order, int amount, String paymentKey, String idempotencyKey) {
        return new Payment(order, amount, PaymentStatus.READY, paymentKey, idempotencyKey);
    }

    public void markDone() {
        requireReady();
        this.status = PaymentStatus.DONE;
    }

    public void markAborted() {
        requireReady();
        this.status = PaymentStatus.ABORTED;
    }

    private void requireReady() {
        if (this.status != PaymentStatus.READY) {
            throw new IllegalStateException("승인 대기 중인 결제가 아닙니다. status=" + status);
        }
    }
}
