package com.example.e_commerce.client.pg;

import com.example.e_commerce.client.pg.dto.request.PgConfirmRequest;
import com.example.e_commerce.client.pg.dto.request.PgCreatePaymentRequest;
import com.example.e_commerce.client.pg.dto.response.PgErrorResponse;
import com.example.e_commerce.client.pg.dto.response.PgPaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Component
public class PgPaymentClient {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final RestClient restClient;

    public PgPaymentClient(@Value("${pg.base-url}") String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(1))
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(3));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    /**
     * 결제 건 생성. 아직 승인 전이라 실패해도 대금은 빠져나가지 않는다.
     * 타임아웃이 나면 paymentKey 를 못 받으므로 복구 조회가 불가능하다.
     */
    public PgPaymentResponse create(Long orderId, int amount) {
        return execute(null, "create", orderId, () -> restClient.post()
                .uri("/v1/payments")
                .body(new PgCreatePaymentRequest(String.valueOf(orderId), amount))
                .retrieve()
                .body(PgPaymentResponse.class));
    }

    /**
     * 결제 승인. 응답을 못 받으면 PG 쪽에서 승인이 완료됐을 수 있으므로
     * {@link PgUnknownResultException} 에 paymentKey 를 담아 복구 조회 경로를 연다.
     */
    public PgPaymentResponse confirm(String paymentKey, Long orderId, int amount) {
        return execute(paymentKey, "confirm", orderId, () -> restClient.post()
                .uri("/v1/payments/confirm")
                .header(IDEMPOTENCY_HEADER, "confirm:" + paymentKey)
                .body(new PgConfirmRequest(paymentKey, String.valueOf(orderId), amount))
                .retrieve()
                .body(PgPaymentResponse.class));
    }

    private <T> T execute(String paymentKey, String operation, Long orderId, Supplier<T> call) {
        long startedAt = System.nanoTime();
        try {
            return call.get();

        } catch (HttpStatusCodeException e) {
            // PG 가 상태코드로 응답함 = 결과 확정.
            throw toPaymentException(e);

        } catch (ResourceAccessException e) {
            // 응답 자체가 없음: read timeout, connect timeout, connection refused.
            // 요청이 PG 에 도달해 처리됐을 수 있으므로 실패로 단정하지 않는다.
            throw toUnknownResult(paymentKey, operation, orderId, startedAt, "응답 없음", e);

        } catch (RestClientException e) {
            // 응답은 받았으나 해석 실패(역직렬화, 예상 못 한 Content-Type 등).
            // 200 이었다면 결제는 성공한 상태이므로 이것도 미상으로 다룬다.
            throw toUnknownResult(paymentKey, operation, orderId, startedAt, "응답 해석 실패", e);
        }
    }

    private PgPaymentException toPaymentException(HttpStatusCodeException e) {
        PgErrorResponse error = e.getResponseBodyAs(PgErrorResponse.class);
        String code = (error != null && error.code() != null) ? error.code() : "UNKNOWN";
        String message = (error != null && error.message() != null) ? error.message() : e.getMessage();
        return new PgPaymentException(code, message, e);
    }

    private PgUnknownResultException toUnknownResult(
            String paymentKey, String operation, Long orderId,
            long startedAt, String reason, RestClientException cause) {

        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
        log.warn("PG 결과 미상: op={}, orderId={}, paymentKey={}, reason={}, elapsed={}ms",
                operation, orderId, paymentKey, reason, elapsedMs, cause);

        return new PgUnknownResultException(paymentKey,
                "PG %s 결과를 확인하지 못했습니다. (%s, %dms)".formatted(operation, reason, elapsedMs),
                cause);
    }
}
