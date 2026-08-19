package com.example.e_commerce.dto.response;

import com.example.e_commerce.entity.Order;
import lombok.Getter;

@Getter
public class OrderResponse {
    private Long orderId;
    private String status;
    private int totalPrice;
    private String paymentKey;

    public OrderResponse(Order order, String paymentKey) {
        this.orderId = order.getId();
        this.status = order.getStatus().name();
        this.totalPrice = order.getTotalAmount();
        this.paymentKey = paymentKey;
    }
}