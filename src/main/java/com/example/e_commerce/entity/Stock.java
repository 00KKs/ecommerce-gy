package com.example.e_commerce.entity;

import com.example.e_commerce.global.exception.OutOfStockException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Getter
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", nullable = false, unique = true)
    private Sku sku;

    @Column(nullable = false)
    private int quantity;

    public Stock(Sku sku, int quantity) {
        this.sku = sku;
        this.quantity = quantity;
    }

    public void increase(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("증가 수량은 1 이상이어야 합니다.");
        }
        this.quantity += amount;
    }

    public void decrease(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("차감 수량은 1 이상이어야 합니다.");
        }
        if(this.quantity < amount) {
            throw new OutOfStockException("재고가 부족합니다.");
        }
        this.quantity -= amount;
    }

    public void restore(int amount) {
        this.quantity += amount;
    }
}
