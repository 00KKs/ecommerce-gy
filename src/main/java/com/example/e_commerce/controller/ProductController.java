package com.example.e_commerce.controller;

import com.example.e_commerce.dto.request.Product.ProductCreateRequest;
import com.example.e_commerce.dto.request.Product.ProductStatusRequest;
import com.example.e_commerce.dto.request.Product.ProductUpdateRequest;
import com.example.e_commerce.dto.response.Product.ProductDetailResponse;
import com.example.e_commerce.dto.response.Product.ProductStatusResponse;
import com.example.e_commerce.dto.response.Product.ProductUpdateResponse;
import com.example.e_commerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{productId}")
    public ProductDetailResponse detail(@PathVariable Long productId) {
        return productService.getProductDetail(productId);
    }

    @PostMapping
    public void create(@RequestBody ProductCreateRequest request) {
        productService.createProduct(request);
    }

    @PostMapping("/{productId}/update")
    public ProductUpdateResponse update(@PathVariable Long productId, @RequestBody ProductUpdateRequest request) {
        return productService.updateProduct(productId, request);
    }

    @PostMapping("/{productId}/status")
    public ProductStatusResponse changeStatus(@PathVariable Long productId,
                                              @RequestBody ProductStatusRequest request) {
        return productService.changeStatus(productId, request);
    }
}
