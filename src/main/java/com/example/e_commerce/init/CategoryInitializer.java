package com.example.e_commerce.init;

import com.example.e_commerce.entity.Category;
import com.example.e_commerce.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CategoryInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() > 0) {
            return;
        }

        Category clothing = save("의류", null);
        Category top = save("상의", clothing);
        save("티셔츠", top);
        save("셔츠", top);
        save("니트", top);
        save("후드/맨투맨", top);

        Category bottom = save("하의", clothing);
        save("청바지", bottom);
        save("슬랙스", bottom);
        save("트레이닝 팬츠", bottom);
        save("반바지", bottom);

        Category outer = save("아우터", clothing);
        save("자켓", outer);
        save("코트", outer);
        save("패딩", outer);

        Category shoes = save("신발", null);
        Category sneakers = save("스니커즈", shoes);
        save("러닝화", sneakers);
        save("캔버스화", sneakers);

        Category dress = save("구두", shoes);
        save("로퍼", dress);
        save("더비", dress);

        Category boots = save("부츠", shoes);
        save("첼시부츠", boots);
        save("워커", boots);
    }

    private Category save(String name, Category parent) {
        return categoryRepository.save(new Category(name, parent));
    }
}
