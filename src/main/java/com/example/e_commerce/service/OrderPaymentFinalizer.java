package com.example.e_commerce.service;

import com.example.e_commerce.entity.Order;
import com.example.e_commerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 결제 결과를 주문에 반영한다. 요청 스레드와 복구 스케줄러가 함께 쓴다.
@Component
@RequiredArgsConstructor
public class OrderPaymentFinalizer {

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final StockService stockService;

    // 승인 확정. 결제 DONE → 주문 CONFIRMED.
    @Transactional
    public Order complete(Long orderId) {
        Order order = getOrder(orderId);
        paymentService.markDone(orderId);
        order.confirm();
        return order;
    }

    // 주문 실패 확정. 주문 CANCELED → 재고 복원 → 결제 ABORTED.
    // 재고 복원은 이 메서드에서만 일어난다. cancel() 가드가 PENDING 에서만 통과하므로 한 번만 실행된다.
    @Transactional
    public void fail(Long orderId) {
        Order order = getOrder(orderId);
        order.cancel();
        order.getItems().forEach(item -> stockService.restore(item.getSkuId(), item.getQuantity()));
        paymentService.markAbortedIfExists(orderId);
    }

    private Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
    }
}
