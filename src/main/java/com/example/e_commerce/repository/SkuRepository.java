package com.example.e_commerce.repository;

import com.example.e_commerce.entity.Sku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SkuRepository extends JpaRepository<Sku, Long> {

    @Query("select distinct s from Sku s join fetch s.product where s.id = :id")
    Optional<Sku> findByIdWithProduct(Long id);

}
