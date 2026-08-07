package com.example.e_commerce.dto.response.Sku;

import lombok.Getter;

@Getter
public class SkuCreateResponse {
    private Long id;

    public SkuCreateResponse(Long id) {
        this.id = id;
    }
}
