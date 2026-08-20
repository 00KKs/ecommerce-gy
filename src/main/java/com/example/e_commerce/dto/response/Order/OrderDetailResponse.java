package com.example.e_commerce.dto.response.Order;

import com.example.e_commerce.entity.Order;
import com.example.e_commerce.entity.OrderItem;
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
    private LocalDateTime orderedAt;

    public OrderDetailResponse(Order order, String paymentKey) {
        this.orderId = order.getId();
        this.status = order.getStatus().name();
        this.totalPrice = order.getTotalAmount();
        this.items = order.getItems().stream().map(OrderItemResponse::new).toList();
        this.paymentKey = paymentKey;
        this.orderedAt = order.getCreatedAt();
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
