package com.example.e_commerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String paymentKey;

    private Payment(Order order, int amount, PaymentStatus status, String paymentKey) {
        this.order = order;
        this.amount = amount;
        this.status = status;
        this.paymentKey = paymentKey;
    }

    public static Payment ready(Order order, int amount, String paymentKey) {
        return new Payment(order, amount, PaymentStatus.READY, paymentKey);
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
