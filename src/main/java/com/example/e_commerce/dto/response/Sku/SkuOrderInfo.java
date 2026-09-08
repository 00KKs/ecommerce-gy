package com.example.e_commerce.dto.response.Sku;

import com.example.e_commerce.entity.Sku;

public record SkuOrderInfo(
        Long skuId,
        String productName,
        String optionName,
        int price
) {
    public SkuOrderInfo(Sku sku) {
        this(sku.getId(), sku.getProduct().getName(), sku.getOptionName(), sku.getPrice());
    }
}
