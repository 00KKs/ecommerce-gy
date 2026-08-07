package com.example.e_commerce.repository;

import com.example.e_commerce.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findBySkuId(Long skuId);
}
