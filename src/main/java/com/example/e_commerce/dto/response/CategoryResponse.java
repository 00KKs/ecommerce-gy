package com.example.e_commerce.dto.response;

import com.example.e_commerce.entity.Category;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class CategoryResponse {

    private Long id;
    private String name;
    private List<CategoryResponse> children;

    public CategoryResponse(Category category, Map<Long, List<Category>> childrenByParentId) {
        this.id = category.getId();
        this.name = category.getName();
        this.children = childrenByParentId
                .getOrDefault(category.getId(), List.of())
                .stream()
                .map(c -> new CategoryResponse(c, childrenByParentId))
                .toList();
    }
}
