package com.example.e_commerce.repository;

import com.example.e_commerce.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 주문의 가장 최근 결제 시도.
    Optional<Payment> findTopByOrderIdOrderByIdDesc(Long orderId);
}
