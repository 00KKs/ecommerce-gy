package com.example.e_commerce.service;

import com.example.e_commerce.dto.request.Sku.SkuCreateRequest;
import com.example.e_commerce.dto.request.Sku.SkuPriceRequest;
import com.example.e_commerce.dto.request.Sku.SkuStatusRequest;
import com.example.e_commerce.dto.response.Sku.*;
import com.example.e_commerce.entity.*;
import com.example.e_commerce.repository.ProductRepository;
import com.example.e_commerce.repository.SkuRepository;
import com.example.e_commerce.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SkuService {

    private final SkuRepository skuRepository;
    private final StockRepository stockRepository;
    private final ProductService productService;

    @Transactional
    public SkuCreateResponse createSku(Long productId, SkuCreateRequest request) {
        Product product = productService.getProduct(productId);

        Sku sku = product.addSku(request.getOptionName(), request.getPrice());
        skuRepository.save(sku);
        stockRepository.save(new Stock(sku, 0));

        return new SkuCreateResponse(sku.getId());
    }

    @Transactional(readOnly = true)
    public List<SkuResponse> getSkus(Long productId) {
        Product product = productService.getProduct(productId);

        if (product.getStatus() != ProductStatus.SELLING) {
            throw new IllegalArgumentException("상품을 찾을 수 없습니다.");
        }

        return product.getSkus().stream()
                .filter(sku -> sku.getStatus() == SkuStatus.SELLING)
                .map(SkuResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SkuResponse> getSkusForAdmin(Long productId) {
        Product product = productService.getProduct(productId);

        return product.getSkus().stream()
                .map(SkuResponse::new)
                .toList();
    }

    @Transactional
    public SkuStatusResponse changeStatus(Long skuId, SkuStatusRequest request) {
        Sku sku = skuRepository.findById(skuId)
                .orElseThrow(() -> new IllegalArgumentException("SKU를 찾을 수 없습니다."));

        sku.changeStatus(SkuStatus.valueOf(request.getStatus()));
        return new SkuStatusResponse(sku);
    }

    @Transactional
    public SkuPriceResponse changePrice(Long skuId, SkuPriceRequest request) {
        Sku sku = skuRepository.findById(skuId)
                .orElseThrow(() -> new IllegalArgumentException("SKU를 찾을 수 없습니다."));
        sku.changePrice(request.getPrice());
        return new SkuPriceResponse(sku);
    }

    @Transactional(readOnly = true)
    public SkuOrderInfo getSkuOrderInfo(Long skuId) {
        Sku sku = skuRepository.findByIdWithProduct(skuId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        return new SkuOrderInfo(sku);
    }
}
