package com.example.e_commerce.service;

import com.example.e_commerce.dto.request.Stock.StockInboundRequest;
import com.example.e_commerce.dto.response.Stock.StockResponse;
import com.example.e_commerce.entity.Stock;
import com.example.e_commerce.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    @Transactional(readOnly = true)
    public StockResponse getStock(Long skuId) {
        Stock stock = stockRepository.findBySkuId(skuId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다."));
        return new StockResponse(stock);
    }

    @Transactional
    public StockResponse inbound(Long skuId, StockInboundRequest request) {
        Stock stock = stockRepository.findBySkuIdForUpdate(skuId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다."));
        stock.increase(request.getQuantity());
        return new StockResponse(stock);
    }

    @Transactional
    public void decrease(Long skuId, int quantity) {
        Stock stock = stockRepository.findBySkuIdForUpdate(skuId)
                .orElseThrow(() -> new IllegalArgumentException("재고 정보를 찾을 수 없습니다."));
        stock.decrease(quantity);
    }

    @Transactional
    public void restore(Long skuId, int quantity) {
        Stock stock = stockRepository.findBySkuIdForUpdate(skuId)
                .orElseThrow(() -> new IllegalArgumentException("재고 정보를 찾을 수 없습니다."));
        stock.restore(quantity);
    }


}
