package com.example.e_commerce.global.exception;

// 같은 멱등키로 다른 내용의 주문을 요청했다.
public class IdempotencyKeyReusedException extends RuntimeException {

    public IdempotencyKeyReusedException() {
        super("같은 요청 키로 다른 주문을 할 수 없습니다.");
    }
}
