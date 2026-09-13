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

    public Payment(Order order, int amount, PaymentStatus status, String paymentKey) {
        this.order = order;
        this.amount = amount;
        this.status = status;
        this.paymentKey = paymentKey;
    }

    public static Payment ready(Order order, int amount, String paymentKey) {
        return new Payment(order, amount, PaymentStatus.READY, paymentKey);
    }

    public void markDone() {
        this.status = PaymentStatus.DONE;
    }

    public void markAborted() {
        this.status = PaymentStatus.ABORTED;
    }
}
