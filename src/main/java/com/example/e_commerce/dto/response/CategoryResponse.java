package com.example.e_commerce.dto.response;

import com.example.e_commerce.entity.Category;
import lombok.Getter;

import java.util.List;

@Getter
public class CategoryResponse {

    private Long id;
    private String name;
    private List<CategoryResponse> children;

    public CategoryResponse(Category category) {
        this.id = category.getId();
        this.name = category.getName();
        this.children = category.getChildren().stream()
                .map(CategoryResponse::new)
                .toList();
    }
}
