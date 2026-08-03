package com.example.e_commerce.dto.response.Sku;

import com.example.e_commerce.entity.Sku;
import lombok.Getter;

@Getter
public class SkuPriceResponse {
    private Long id;
    private int price;

    public SkuPriceResponse(Sku sku) {
        this.id = sku.getId();
        this.price = sku.getPrice();
    }
}
