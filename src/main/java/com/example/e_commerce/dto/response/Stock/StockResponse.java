package com.example.e_commerce.dto.response.Stock;

import com.example.e_commerce.entity.Stock;
import lombok.Getter;

@Getter
public class StockResponse {
    private Long skuId;
    private int quantity;

    public StockResponse(Stock stock) {
        this.skuId = stock.getSku().getId();
        this.quantity = stock.getQuantity();
    }
}
