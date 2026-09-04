package com.example.e_commerce.service;

import com.example.e_commerce.dto.request.Product.ProductCreateRequest;
import com.example.e_commerce.dto.request.Product.ProductStatusRequest;
import com.example.e_commerce.dto.request.Product.ProductUpdateRequest;
import com.example.e_commerce.dto.response.Product.ProductDetailResponse;
import com.example.e_commerce.dto.response.Product.ProductListResponse;
import com.example.e_commerce.dto.response.Product.ProductStatusResponse;
import com.example.e_commerce.dto.response.Product.ProductUpdateResponse;
import com.example.e_commerce.entity.*;
import com.example.e_commerce.repository.CategoryRepository;
import com.example.e_commerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.OptionalInt;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    @Transactional
    public void createProduct(ProductCreateRequest request) {
        Category category = categoryService.getLeafCategory(request.getCategoryId());



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

    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        if (product.getStatus() != ProductStatus.SELLING) {
            throw new IllegalArgumentException("판매 중인 상품이 아닙니다.");
        }

        List<Sku> sellingSkus = product.getSkus().stream()
                .filter(sku -> sku.getStatus() == SkuStatus.SELLING)
                .toList();

        return new ProductDetailResponse(product, sellingSkus);
    }

    @Transactional(readOnly = true)
    public List<ProductListResponse> getProductsByCategory(Long categoryId) {
        List<Product> products = productRepository.findByCategoryIdAndStatusWithSkus(categoryId, ProductStatus.SELLING);

        return products.stream()
                .map(product -> {
                    OptionalInt lowestPrice = product.getSkus().stream()
                            .filter(sku -> sku.getStatus() == SkuStatus.SELLING)
                            .mapToInt(Sku::getPrice)
                            .min();
                    return new ProductListResponse(product.getId(), product.getName(),
                            lowestPrice.isPresent() ? lowestPrice.getAsInt() : null);
                })
                .toList();
    }

    @Transactional
    public ProductStatusResponse changeStatus(Long productId, ProductStatusRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        product.changeStatus(ProductStatus.valueOf(request.getStatus()));
        return new ProductStatusResponse(product);
    }
}
