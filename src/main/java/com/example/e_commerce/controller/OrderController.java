package com.example.e_commerce.controller;

import com.example.e_commerce.dto.request.Order.OrderCreateRequest;
import com.example.e_commerce.dto.response.OrderCreateResponse;
import com.example.e_commerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public OrderCreateResponse createOrder() {
        return orderService.createOrder();
    }

}
