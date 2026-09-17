package com.example.e_commerce.global.exception;

import com.example.e_commerce.client.pg.PgPaymentException;
import com.example.e_commerce.client.pg.PgUnknownResultException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("잘못된 요청입니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("BAD_REQUEST", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("SERVER_ERROR", "요청을 처리할 수 없습니다."));
    }

    @ExceptionHandler(OutOfStockException.class)
    public ResponseEntity<ErrorResponse> handleOutOfStock(OutOfStockException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("OUT_OF_STOCK", e.getMessage()));
    }

    @ExceptionHandler(PgPaymentException.class)
    public ResponseEntity<ErrorResponse> handlePgPayment(PgPaymentException e) {
        if (e.isRejected()) {
            log.info("결제 거절: code={}, message={}", e.getCode(), e.getMessage());
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(new ErrorResponse("PAYMENT_REJECTED",
                            "결제가 거절되었습니다. 카드사에 문의해주세요."));
        }

        log.error("PG 연동 오류: code={}, message={}", e.getCode(), e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("PAYMENT_ERROR", "결제 처리 중 오류가 발생했습니다."));
    }

    @ExceptionHandler(PgUnknownResultException.class)
    public ResponseEntity<ErrorResponse> handlePgUnknown(PgUnknownResultException e) {

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ErrorResponse("PAYMENT_UNCONFIRMED",
                        "결제 결과를 확인하는 중입니다. 주문 내역에서 상태를 확인해주세요. "
                                + "다시 결제하지 마세요."));
    }
}
