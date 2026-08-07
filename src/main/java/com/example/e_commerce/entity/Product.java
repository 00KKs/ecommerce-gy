package com.example.e_commerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.PERSIST)
    private List<Sku> skus = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    public Product(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.status = ProductStatus.DRAFT;
    }

    public void updateInfo(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Sku addSku(String optionName, int price) {
        boolean isDuplicated = skus.stream()
                .anyMatch(sku -> sku.getOptionName().equals(optionName));
        if(isDuplicated) {
            throw new IllegalArgumentException("이미 존재하는 옵션입니다.");
        }
        Sku sku = new Sku(this, optionName, price);
        skus.add(sku);
        return sku;
    }

    public void changeStatus(ProductStatus newStatus) {
        if(newStatus == ProductStatus.DRAFT) {
            throw new IllegalArgumentException("DRAFT로는 전환 불가합니다.");
        }
        if(newStatus == ProductStatus.SELLING) {
            boolean hasSelling = skus.stream()
                    .anyMatch(sku -> sku.getStatus() == SkuStatus.SELLING);
            if(!hasSelling) {
                throw new IllegalArgumentException("판매중인 옵션이 최소 1개 있어야합니다.");
            }
        }
        this.status = newStatus;
    }
}
