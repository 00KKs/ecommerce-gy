package com.example.e_commerce.service;

import com.example.e_commerce.dto.response.CategoryResponse;
import com.example.e_commerce.entity.Category;
import com.example.e_commerce.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        List<Category> all = categoryRepository.findAll();

        Map<Long, List<Category>> childrenByParentId = all.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        return all.stream()
                .filter(c -> c.getParent() == null)
                .map(c -> new CategoryResponse(c, childrenByParentId))
                .toList();
    }
}
