package com.example.e_commerce.repository;

import com.example.e_commerce.entity.Product;
import com.example.e_commerce.entity.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("select distinct p from Product p " +
            "left join fetch p.skus " +
            "where p.category.id = :categoryId and p.status = :status")
    List<Product> findByCategoryIdAndStatusWithSkus(Long categoryId, ProductStatus status);
}
