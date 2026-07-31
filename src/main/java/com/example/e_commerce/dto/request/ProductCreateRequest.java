package com.example.e_commerce.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductCreateRequest {
    private Long categoryId;
    private String name;
    private String description;
}
