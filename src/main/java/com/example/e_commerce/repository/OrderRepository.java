package com.example.e_commerce.repository;

import com.example.e_commerce.entity.Order;
import com.example.e_commerce.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 복구 스케줄러용. 유예 시간이 지난 결제 대기 주문을 오래된 순으로 50건씩 가져온다.
    List<Order> findTop50ByStatusAndCreatedAtBeforeOrderByIdAsc(OrderStatus status, LocalDateTime createdAt);

}
