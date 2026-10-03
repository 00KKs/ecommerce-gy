package com.example.e_commerce.dto.response.Order;

import com.example.e_commerce.entity.OrderStatus;
import lombok.Getter;

@Getter
public class OrderCreateResponse {
    private Long orderId;
    private String status;
    private int totalPrice;
    private String paymentKey;

    public OrderCreateResponse(Long orderId, OrderStatus status, int totalPrice, String paymentKey) {
        this.orderId = orderId;
        this.status = status.name();
        this.totalPrice = totalPrice;
        this.paymentKey = paymentKey;
    }
}
