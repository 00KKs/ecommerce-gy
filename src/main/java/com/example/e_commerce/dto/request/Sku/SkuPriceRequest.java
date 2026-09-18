package com.example.e_commerce.dto.request.Sku;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SkuPriceRequest {
    @Min(value = 1, message = "은(는) 0보다 커야 합니다.")
    private int price;
}
