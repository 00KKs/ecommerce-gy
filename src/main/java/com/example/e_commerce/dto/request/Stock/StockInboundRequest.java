package com.example.e_commerce.dto.request.Stock;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StockInboundRequest {
    @Positive(message = "입고 수량은 1 이상이어야 합니다.")
    private int quantity;
}
