package com.example.e_commerce.entity;

public enum OrderStatus {
    PAYMENT_PENDING,
    PAYMENT_FAILED,   // 결제 시도 실패. 결제 기한 안에 재결제할 수 있다. 재고는 잡아둔다.
    PAYMENT_UNKNOWN,
    CANCELED,
    CONFIRMED,
    SHIPPED
}
