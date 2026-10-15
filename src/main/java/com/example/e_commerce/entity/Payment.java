package com.example.e_commerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 결제 시도 1건. PG 를 호출하기 전에 REQUESTED 로 먼저 커밋해 멱등키와 시도 기록을 남긴다.
@Entity
@Table(indexes = @Index(name = "idx_payment_order_id", columnList = "order_id"),
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_order_idempotency_key", columnNames = {"order_id", "idempotency_key"}),
                @UniqueConstraint(name = "uk_payment_payment_key", columnNames = {"payment_key"})
        })
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

    // PG 가 발급한다. 결제 생성 응답을 받기 전(REQUESTED)이나 생성에 실패한 시도는 null 이다.
    @Column(name = "payment_key")
    private String paymentKey;

    // 실패한 결제 시도의 사유
    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason")
    private PaymentFailureReason failureReason;

    // 재결제 요청의 멱등키. 최초 주문의 결제 시도는 주문 멱등키로 보호되므로 null.
    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    private Payment(Order order, int amount, String idempotencyKey) {
        this.order = order;
        this.amount = amount;
        this.status = PaymentStatus.REQUESTED;
        this.idempotencyKey = idempotencyKey;
    }

    public static Payment request(Order order, int amount, String idempotencyKey) {
        return new Payment(order, amount, idempotencyKey);
    }

    // PG 결제 생성 성공. 발급받은 paymentKey 를 기록하고 승인 대기로 넘어간다.
    public void markReady(String paymentKey) {
        if (this.status != PaymentStatus.REQUESTED) {
            throw new IllegalStateException("결제 생성 대기 중인 결제 시도가 아닙니다. status=" + status);
        }
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.READY;
    }

    public void markDone() {
        if (this.status != PaymentStatus.READY) {
            throw new IllegalStateException("승인 대기 중인 결제가 아닙니다. status=" + status);
        }
        this.status = PaymentStatus.DONE;
    }

    // 실패 확정. PG 결제 생성 단계(REQUESTED)와 승인 단계(READY) 모두에서 올 수 있다.
    public void markAborted(PaymentFailureReason reason) {
        if (this.status != PaymentStatus.REQUESTED && this.status != PaymentStatus.READY) {
            throw new IllegalStateException("진행 중인 결제 시도가 아닙니다. status=" + status);
        }
        this.status = PaymentStatus.ABORTED;
        this.failureReason = reason;
    }
}