package com.example.e_commerce.dto.response.Order;

import com.example.e_commerce.entity.Order;
import com.example.e_commerce.entity.OrderItem;
import com.example.e_commerce.entity.Payment;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class OrderDetailResponse {
    private Long orderId;
    private String status;
    private int totalPrice;
    private List<OrderItemResponse> items;
    private String paymentKey;
    private String paymentStatus;
    private LocalDateTime orderedAt;

    public OrderDetailResponse(Order order, Payment payment) {
        this.orderId = order.getId();
        this.status = order.getStatus().name();
        this.totalPrice = order.getTotalAmount();
        this.items = order.getItems().stream().map(OrderItemResponse::new).toList();
        this.orderedAt = order.getCreatedAt();

        // 결제 레코드는 없을 수 있다(create 단계에서 실패한 주문).
        this.paymentKey = payment != null ? payment.getPaymentKey() : null;
        this.paymentStatus = payment != null ? payment.getStatus().name() : null;
    }

    @Getter
    public static class OrderItemResponse {
        private String productName;
        private String optionName;
        private int unitPrice;
        private int quantity;

        public OrderItemResponse(OrderItem item) {
            this.productName = item.getProductName();
            this.optionName = item.getOptionName();
            this.unitPrice = item.getPrice();
            this.quantity = item.getQuantity();
        }
    }
}
