package com.example.e_commerce.global.exception;

import lombok.Getter;

@Getter
public class OrderAlreadyPaidException extends RuntimeException {

    private final Long orderId;

    public OrderAlreadyPaidException(Long orderId) {
        super("이미 결제된 주문입니다.");
        this.orderId = orderId;
    }
}
