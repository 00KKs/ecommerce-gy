package com.example.e_commerce.dto.response;

import com.example.e_commerce.entity.Product;
import lombok.Getter;

@Getter
public class ProductUpdateResponse {
    private Long id;
    private String name;
    private String description;

    public ProductUpdateResponse(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.description = product.getDescription();
    }
}
