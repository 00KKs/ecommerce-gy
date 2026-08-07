package com.example.e_commerce.controller;

import com.example.e_commerce.dto.response.Product.ProductDetailResponse;
import com.example.e_commerce.dto.response.Product.ProductListResponse;
import com.example.e_commerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{productId}")
    public ProductDetailResponse detail(@PathVariable Long productId) {
        return productService.getProductDetail(productId);
    }

    @GetMapping(params = "categoryId")
    public List<ProductListResponse> listByCategory(@RequestParam Long categoryId) {
        return productService.getProductsByCategory(categoryId);
    }
}
