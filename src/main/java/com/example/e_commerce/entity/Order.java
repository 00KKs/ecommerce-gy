package com.example.e_commerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private String recipientPhone;

    @Column(nullable = false)
    private String address;

    private String deliveryRequest;

    public Order(Member member, String recipientName, String recipientPhone,
                 String address, String deliveryRequest) {
        this.member = member;
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
        this.status = OrderStatus.CONFIRMED;
    }

    public void ship() {
        this.status = OrderStatus.SHIPPED;
    }
}
