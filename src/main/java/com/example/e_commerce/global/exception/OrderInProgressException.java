package com.example.e_commerce.global.exception;

import lombok.Getter;

// 같은 멱등키의 주문이 아직 결제 확정 전이다(처리 중이거나 복구 대기).
@Getter
public class OrderInProgressException extends RuntimeException {

    private final Long orderId;

    public OrderInProgressException(Long orderId) {
        super("결제 결과를 확인하는 중입니다.");
        this.orderId = orderId;
    }
}
