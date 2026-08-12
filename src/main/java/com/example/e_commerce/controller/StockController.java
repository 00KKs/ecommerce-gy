package com.example.e_commerce.controller;

import com.example.e_commerce.dto.request.Stock.StockInboundRequest;
import com.example.e_commerce.dto.response.Stock.StockResponse;
import com.example.e_commerce.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/skus/{skuId}/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping
    public StockResponse get(@PathVariable Long skuId) {
        return stockService.getStock(skuId);
    }

    @PostMapping("/inbound")
    public StockResponse inbound(@PathVariable Long skuId,
                                 @Valid @RequestBody StockInboundRequest request) {
        return stockService.inbound(skuId, request);
    }
}
