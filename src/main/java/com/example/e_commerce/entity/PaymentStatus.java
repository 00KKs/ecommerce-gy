package com.example.e_commerce.entity;

public enum PaymentStatus {
    REQUESTED, // 결제 시도 시작
    READY, // PG 결제 생성 완료. 승인 대기
    DONE, // 결제 완료
    CANCELED, // 결제 완료 후 취소
    ABORTED //결제 실패
}
