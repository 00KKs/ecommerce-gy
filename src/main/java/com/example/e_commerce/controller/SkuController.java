package com.example.e_commerce.controller;

import com.example.e_commerce.dto.response.Sku.SkuResponse;
import com.example.e_commerce.service.SkuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SkuController {

    private final SkuService skuService;

    @GetMapping("/api/products/{productId}/skus")
    public List<SkuResponse> list(@PathVariable Long productId) {
        return skuService.getSkus(productId);
    }
}
