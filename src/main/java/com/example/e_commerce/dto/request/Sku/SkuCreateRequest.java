package com.example.e_commerce.dto.request.Sku;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SkuCreateRequest {
    private String optionName;
    private int price;
}
