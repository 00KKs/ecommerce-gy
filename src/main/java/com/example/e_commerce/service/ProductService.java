package com.example.e_commerce.service;

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
    public Long createProduct(String name, String description, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

        if(!category.getChildren().isEmpty()) {
            throw new IllegalArgumentException("상품은 최하위 카테고리만 선택가능합니다.");
        }

        Product product = new Product(name, description, category);
        productRepository.save(product);

        return product.getId();

    }
}
