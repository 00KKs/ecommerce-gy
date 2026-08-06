package com.example.e_commerce.dto.response.Product;

import lombok.Getter;

@Getter
public class ProductListResponse {
    private Long id;
    private String name;
    private Integer lowestPrice;

    public ProductListResponse(Long id, String name, Integer lowestPrice) {
        this.id = id;
        this.name = name;
        this.lowestPrice = lowestPrice;
    }
}
