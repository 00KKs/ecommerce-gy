package com.example.e_commerce.controller;

import com.example.e_commerce.auth.CustomUserDetail;
import com.example.e_commerce.dto.request.Order.OrderCreateRequest;
import com.example.e_commerce.dto.response.Order.OrderDetailResponse;
import com.example.e_commerce.dto.response.OrderCreateResponse;
import com.example.e_commerce.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderCreateResponse createOrder(@AuthenticationPrincipal CustomUserDetail user,
                                           @Valid @RequestBody OrderCreateRequest request) {
        return orderService.createOrder(user.getMemberId(), request);
    }

    @GetMapping("/{orderId}")
    public OrderDetailResponse detail(@AuthenticationPrincipal CustomUserDetail user,
                                      @PathVariable Long orderId) {
        return orderService.getOrder(user.getMemberId(), orderId);
    }

}
