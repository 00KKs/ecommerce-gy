package com.example.e_commerce.entity;

public enum PaymentStatus {
    READY, // 결제 요청 완료 후 결제 준비 완료
    DONE, // 결제 완료
    CANCELED, // 결제 완료 후 취소
    ABORTED //결제 실패
}
