package com.example.e_commerce.dto.response.Sku;

import com.example.e_commerce.entity.Sku;
import lombok.Getter;

@Getter
public class SkuStatusResponse {
    private Long id;
    private String status;

    public SkuStatusResponse(Sku sku) {
        this.id = sku.getId();
        this.status = sku.getStatus().name();
    }
}
