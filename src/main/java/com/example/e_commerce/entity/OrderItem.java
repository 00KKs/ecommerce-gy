package com.example.e_commerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Long skuId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private String optionName;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int quantity;

    public OrderItem(Order order, Long skuId, String productName, String optionName,
                     int price, int quantity) {
        this.order = order;
        this.skuId = skuId;
        this.productName = productName;
        this.optionName = optionName;
        this.price = price;
        this.quantity = quantity;
    }

    public int getTotalPrice() {
        return price * quantity;
    }
}
