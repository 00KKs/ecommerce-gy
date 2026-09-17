package com.example.e_commerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
public class Sku {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String optionName;

    @Column(nullable = false)
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SkuStatus status;

    public Sku(Product product, String optionName, int price) {
        validatePrice(price);
        this.product = product;
        this.optionName = optionName;
        this.price = price;
        this.status = SkuStatus.STOPPED;
    }

    public void changeStatus(SkuStatus status) {
        this.status = status;
    }

    public void changePrice(int price) {
        validatePrice(price);
        this.price = price;
    }


    private static void validatePrice(int price) {
        if (price <= 0) {
            throw new IllegalArgumentException("가격은 0보다 커야 합니다.");
        }
    }
}
