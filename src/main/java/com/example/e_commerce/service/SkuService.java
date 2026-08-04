package com.example.e_commerce.service;

import com.example.e_commerce.dto.request.Sku.SkuCreateRequest;
import com.example.e_commerce.dto.request.Sku.SkuPriceRequest;
import com.example.e_commerce.dto.request.Sku.SkuStatusRequest;
import com.example.e_commerce.dto.response.Sku.SkuCreateResponse;
import com.example.e_commerce.dto.response.Sku.SkuPriceResponse;
import com.example.e_commerce.dto.response.Sku.SkuResponse;
import com.example.e_commerce.dto.response.Sku.SkuStatusResponse;
import com.example.e_commerce.entity.Product;
import com.example.e_commerce.entity.Sku;
import com.example.e_commerce.entity.SkuStatus;
import com.example.e_commerce.repository.ProductRepository;
import com.example.e_commerce.repository.SkuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SkuService {

    private final ProductRepository productRepository;
    private final SkuRepository skuRepository;

    @Transactional
    public SkuCreateResponse createSku(Long productId, SkuCreateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        Sku sku = product.addSku(request.getOptionName(), request.getPrice());
        skuRepository.save(sku);

        return new SkuCreateResponse(sku.getId());
    }

    @Transactional(readOnly = true)
    public List<SkuResponse> getSkus(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

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
}
