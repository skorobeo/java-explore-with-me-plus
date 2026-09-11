package ru.practicum.ewm.category.service;

import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;

import java.util.List;

public interface CategoryService {
    List<CategoryDto> getCategories(int from, int size);

    CategoryDto getCategoryId(Long catId);

    CategoryDto postAdminCategory(NewCategoryDto dto);

    CategoryDto patchAdminCategory(Long catId, CategoryDto dto);

    void deleteCategory(Long catId);
}
