package com.example.e_commerce.dto.response.Product;

import com.example.e_commerce.entity.Product;
import lombok.Getter;

@Getter
public class ProductStatusResponse {
    private Long id;
    private String status;

    public ProductStatusResponse(Product product) {
        this.id = product.getId();
        this.status = product.getStatus().name();
    }
}
