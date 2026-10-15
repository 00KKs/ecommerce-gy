package com.example.e_commerce.repository;

import com.example.e_commerce.entity.Order;
import com.example.e_commerce.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 만료 스케줄러용. 생성 후 일정 시간이 지난 주문을 오래된 순으로 50건씩 가져온다.
    List<Order> findTop50ByStatusAndCreatedAtBeforeOrderByIdAsc(OrderStatus status, LocalDateTime createdAt);

    // 복구 스케줄러용. 결제 시도를 시작한 지 유예 시간이 지난 주문을 오래된 순으로 50건씩 가져온다.
    List<Order> findTop50ByStatusAndPaymentStartedAtBeforeOrderByIdAsc(OrderStatus status, LocalDateTime paymentStartedAt);

    Optional<Order> findByMemberIdAndIdempotencyKey(Long memberId, String idempotencyKey);

    // 결제 결과 반영용. 같은 주문의 상태 전이가 동시에 일어나지 않도록 행을 잠근다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

}
