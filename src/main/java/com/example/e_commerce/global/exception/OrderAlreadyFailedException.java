package com.example.e_commerce.global.exception;

import lombok.Getter;

// 같은 멱등키의 주문이 이미 실패로 확정됐다. 다시 주문하려면 새 키로 시도해야 한다.
@Getter
public class OrderAlreadyFailedException extends RuntimeException {

    private final Long orderId;

    public OrderAlreadyFailedException(Long orderId) {
        super("실패한 주문입니다. 다시 주문하려면 새로 시도해주세요.");
        this.orderId = orderId;
    }
}
