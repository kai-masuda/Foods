package com.example.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.entity.Category;
import com.example.repository.CategoryRepository;

@Service
public class CategoryService {
    
    @Autowired
    private CategoryRepository categoryRepository;

    // カテゴリー一覧取得
    public List<Category> getAllCategories() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }
    
    // 【追記】カテゴリーの保存
    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }
    
}
