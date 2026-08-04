package com.example.e_commerce.dto.response.Product;

import lombok.Getter;

@Getter
public class ProductListResponse {
    private Long id;
    private String name;
    private int lowestPrice;

    public ProductListResponse(Long id, String name, int lowestPrice) {
        this.id = id;
        this.name = name;
        this.lowestPrice = lowestPrice;
    }
}
