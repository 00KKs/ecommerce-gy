package com.example.e_commerce.global.exception;

import com.example.e_commerce.entity.PaymentFailureReason;
import lombok.Getter;

// 결제 시도가 실패해 주문이 PAYMENT_FAILED 가 됐다. 결제 기한 안에 orderId 로 재결제할 수 있다.
@Getter
public class OrderPaymentFailedException extends RuntimeException {

    private final Long orderId;
    private final PaymentFailureReason reason;

    public OrderPaymentFailedException(Long orderId, PaymentFailureReason reason) {
        this(orderId, reason, null);
    }

    public OrderPaymentFailedException(Long orderId, PaymentFailureReason reason, Throwable cause) {
        super(reason != null ? reason.getMessage() : "결제에 실패했습니다. 결제 기한 안에 재결제할 수 있습니다.", cause);
        this.orderId = orderId;
        this.reason = reason;
    }
}
