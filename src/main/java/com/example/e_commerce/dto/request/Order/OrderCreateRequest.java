package com.example.e_commerce.dto.request.Order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OrderCreateRequest {

    @NotNull
    private Long skuId;

    @Min(1)
    private int quantity;
}
