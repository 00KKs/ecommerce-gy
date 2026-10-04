package com.example.e_commerce.service;

import com.example.e_commerce.entity.Order;
import com.example.e_commerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 결제 결과를 주문에 반영한다. 요청 스레드와 복구 스케줄러가 함께 쓴다.
// 모든 전이는 주문 행에 비관적 락을 건 뒤 수행한다. 동시에 들어오면 한쪽은 기다렸다가 바뀐 상태를 보고 가드에 걸린다.
@Component
@RequiredArgsConstructor
public class OrderPaymentFinalizer {

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final StockService stockService;

    // 승인 확정. 주문 CONFIRMED → 결제 DONE.
    @Transactional
    public Order complete(Long orderId) {
        Order order = getOrderForUpdate(orderId);
        order.confirm();
        paymentService.markDone(orderId);
        return order;
    }

    // 주문 실패 확정. 주문 CANCELED → 재고 복원 → 결제 ABORTED.
    // 재고 복원은 이 메서드에서만 일어난다. 주문 락 + cancel() 가드로 동시에 호출돼도 한 번만 실행된다.
    @Transactional
    public void fail(Long orderId) {
        Order order = getOrderForUpdate(orderId);
        order.cancel();
        order.getItems().forEach(item -> stockService.restore(item.getSkuId(), item.getQuantity()));
        paymentService.markAbortedIfExists(orderId);
    }

    @Transactional
    public void holdForReview(Long orderId) {
        getOrderForUpdate(orderId).markPaymentUnknown();
        // 재고는 건드리지 않는다. 결제가 됐는지 모르기 때문.
    }

    private Order getOrderForUpdate(Long orderId) {
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
    }
}
