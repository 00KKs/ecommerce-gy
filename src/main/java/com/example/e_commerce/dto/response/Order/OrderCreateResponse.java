package com.example.e_commerce.dto.response;

import com.example.e_commerce.entity.Order;
import lombok.Getter;

@Getter
public class OrderCreateResponse {
    private Long orderId;
    private String status;
    private int totalPrice;
    private String paymentKey;

    public OrderCreateResponse(Order order, String paymentKey) {
        this.orderId = order.getId();
        this.status = order.getStatus().name();
        this.totalPrice = order.getTotalAmount();
        this.paymentKey = paymentKey;
    }
}