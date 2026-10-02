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
@Table(name = "orders")
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "member_id")
    private Long memberId;

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

    public Order(Long memberId, String recipientName, String recipientPhone,
                 String address, String deliveryRequest) {
        this.memberId = memberId;
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

    private void requirePaymentPending() {
        if (this.status != OrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("결제 대기 중인 주문이 아닙니다. status=" + status);
        }

    public void ship() {
        this.status = OrderStatus.SHIPPED;
    }
}
