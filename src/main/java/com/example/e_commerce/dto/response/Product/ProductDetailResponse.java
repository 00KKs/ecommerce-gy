package com.example.e_commerce.dto.response.Product;

import com.example.e_commerce.dto.response.Sku.SkuResponse;
import com.example.e_commerce.entity.Product;
import com.example.e_commerce.entity.Sku;
import lombok.Getter;

import java.util.List;

@Getter
public class ProductDetailResponse {

    private Long id;
    private Long categoryId;
    private String name;
    private String description;
    private String status;
    private List<SkuResponse> skus;

    public ProductDetailResponse(Product product, List<Sku> sellingSkus) {
        this.id = product.getId();
        this.categoryId = product.getCategory().getId();
        this.name = product.getName();
        this.description = product.getDescription();
        this.status = product.getStatus().name();
        this.skus = sellingSkus.stream().map(SkuResponse::new).toList();
    }
}
