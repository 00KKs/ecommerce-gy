package com.example.e_commerce.repository;

import com.example.e_commerce.entity.Sku;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkuRepository extends JpaRepository<Sku, Long> {
}
