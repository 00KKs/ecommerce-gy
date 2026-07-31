package com.example.e_commerce.controller;

import com.example.e_commerce.dto.request.ProductCreateRequest;
import com.example.e_commerce.dto.request.ProductUpdateRequest;
import com.example.e_commerce.dto.response.ProductUpdateResponse;
import com.example.e_commerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public void create(@RequestBody ProductCreateRequest request) {
        productService.createProduct(request);
    }

    @PostMapping("/{productId}/update")
    public ProductUpdateResponse update(@PathVariable Long productId, @RequestBody ProductUpdateRequest request) {
        return productService.updateProduct(productId, request);
    }
}
