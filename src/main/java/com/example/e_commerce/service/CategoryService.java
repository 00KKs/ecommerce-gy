package com.example.e_commerce.service;

import com.example.e_commerce.dto.response.CategoryResponse;
import com.example.e_commerce.entity.Category;
import com.example.e_commerce.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        List<Category> all = categoryRepository.findAllWithParent();

        return all.stream()
                .filter(c -> c.getParent() == null)
                .map(CategoryResponse::new)
                .toList();
    }
}
