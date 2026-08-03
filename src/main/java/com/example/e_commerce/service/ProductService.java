package com.example.e_commerce.service;

import com.example.e_commerce.dto.request.ProductCreateRequest;
import com.example.e_commerce.dto.request.ProductUpdateRequest;
import com.example.e_commerce.dto.response.ProductUpdateResponse;
import com.example.e_commerce.entity.Category;
import com.example.e_commerce.entity.Product;
import com.example.e_commerce.repository.CategoryRepository;
import com.example.e_commerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public void createProduct(ProductCreateRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

        if(!category.getChildren().isEmpty()) {
            throw new IllegalArgumentException("상품은 최하위 카테고리만 선택가능합니다.");
        }

        Product product = new Product(request.getName(), request.getDescription(), category);
        productRepository.save(product);
    }

    @Transactional
    public ProductUpdateResponse updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        product.updateInfo(request.getName(), request.getDescription());
        return new ProductUpdateResponse(product);
    }
}
