package com.example.e_commerce.dto.response.Sku;

import com.example.e_commerce.entity.Sku;
import lombok.Getter;

@Getter
public class SkuResponse {
    private Long id;
    private String optionName;
    private int price;
    private String status;

    public SkuResponse(Sku sku) {
        this.id = sku.getId();
        this.optionName = sku.getOptionName();
        this.price = sku.getPrice();
        this.status = sku.getStatus().name();
    }
}
