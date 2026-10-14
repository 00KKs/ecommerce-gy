package com.example.e_commerce.entity;

// 결제 시도가 실패한 이유
public enum PaymentFailureReason {

    PAYMENT_DECLINED("결제가 거절되었습니다. 다른 카드나 결제 수단으로 다시 시도해주세요."),
    PG_UNAVAILABLE("결제 서비스에 일시적인 문제가 있습니다. 잠시 후 다시 시도해주세요."),
    PAYMENT_ERROR("결제 처리 중 오류가 발생했습니다. 잠시 후 다시 시도하고, 계속되면 문의해주세요.");

    private final String message;

    PaymentFailureReason(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
