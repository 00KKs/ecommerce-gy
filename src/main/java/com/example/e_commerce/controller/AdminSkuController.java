package com.example.e_commerce.controller;

import com.example.e_commerce.dto.request.Sku.SkuCreateRequest;
import com.example.e_commerce.dto.request.Sku.SkuPriceRequest;
import com.example.e_commerce.dto.request.Sku.SkuStatusRequest;
import com.example.e_commerce.dto.response.Sku.SkuCreateResponse;
import com.example.e_commerce.dto.response.Sku.SkuPriceResponse;
import com.example.e_commerce.dto.response.Sku.SkuResponse;
import com.example.e_commerce.dto.response.Sku.SkuStatusResponse;
import com.example.e_commerce.service.SkuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminSkuController {

    private final SkuService skuService;

    @PostMapping("/products/{productId}/skus")
    public SkuCreateResponse create(@PathVariable Long productId,
                                    @RequestBody SkuCreateRequest request) {
        return skuService.createSku(productId, request);
    }

    @GetMapping("/products/{productId}/skus")
    public List<SkuResponse> listForAdmin(@PathVariable Long productId) {
        return skuService.getSkusForAdmin(productId);
    }

    @PostMapping("/skus/{skuId}/status")
    public SkuStatusResponse changeStatus(@PathVariable Long skuId,
                                          @RequestBody SkuStatusRequest request) {
        return skuService.changeStatus(skuId, request);
    }

    @PostMapping("/skus/{skuId}/price")
    public SkuPriceResponse changePrice(@PathVariable Long skuId,
                                        @RequestBody SkuPriceRequest request) {
        return skuService.changePrice(skuId, request);
    }
}
