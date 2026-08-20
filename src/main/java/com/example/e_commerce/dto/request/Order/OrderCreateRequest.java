package com.example.e_commerce.dto.request.Order;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class OrderCreateRequest {

    private Long skuId;
    private int quantity;
}
