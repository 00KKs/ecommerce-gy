package com.example.e_commerce.global.exception;

import lombok.Getter;

// 결제 시도가 실패해 주문이 PAYMENT_FAILED 가 됐다. 결제 기한 안에 orderId 로 재결제할 수 있다.
@Getter
public class OrderPaymentFailedException extends RuntimeException {

    private final Long orderId;

    public OrderPaymentFailedException(Long orderId) {
        this(orderId, null);
    }

    public OrderPaymentFailedException(Long orderId, Throwable cause) {
        super("결제에 실패했습니다. 결제 기한 안에 재결제할 수 있습니다.", cause);
        this.orderId = orderId;
    }
}
