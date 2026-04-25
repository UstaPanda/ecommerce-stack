package com.ecommerce.main.category;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> getRootCategories() {
        return categoryRepository.findByParentIsNull();
    }

    public List<Category> getChildren(Long parentId) {
        return categoryRepository.findByParentId(parentId);
    }

    public List<Category> getAll() {
        return categoryRepository.findAll();
    }

    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));
    }

    public Category create(String name, Long parentId) {
        Category category = new Category();
        category.setName(name);
        if (parentId != null) {
            category.setParent(getById(parentId));
        }
        return categoryRepository.save(category);
    }

    public Category update(Long id, String name, Long parentId) {
        Category category = getById(id);
        category.setName(name);
        if (parentId != null) {
            category.setParent(getById(parentId));
        } else {
            category.setParent(null);
        }
        return categoryRepository.save(category);
    }

    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }
}
