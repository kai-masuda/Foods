package com.example.service;

import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.entity.Food;
import com.example.repository.FoodRepository;

@Service
public class FoodService {
    
    @Autowired
    private FoodRepository foodRepository;
    
    // 食材の一覧取得 (※特定のユーザーのものだけを取得するように変更)
    public List<Food> getAllFoodsByUserId(Long userId, Sort sort) {
        return foodRepository.findByUserId(userId, sort);
    }
    
    
    // 食材を1件取得
    public Optional<Food> getFoodById(Long id) {
        return foodRepository.findById(id);
    }
    
    // 食材の追加・更新
    @Transactional
    public Food saveFood(Food food) {
        return foodRepository.save(food);
    }
    
    // 食材の消費
    @Transactional
    public void deleteFood(Long id) {
        foodRepository.deleteById(id);
    }
    
    // ★ユーザーごとに検索・絞り込みを統合した新しいメソッド
    public List<Food> searchFoodsByUserId(Long userId, String keyword, String categoryName, Sort sort){
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasCategory = categoryName != null && !categoryName.isBlank();
        
        // 1. キーワード、カテゴリ絞り込みあり
        if(hasKeyword && hasCategory) {
            return foodRepository.searchByUserIdAndNameAndCategoryName(userId, keyword, categoryName, sort);
        // 2. キーワードあり
        } else if(hasKeyword){
            return foodRepository.searchByUserIdAndName(userId, keyword, sort);
        // 3. カテゴリ絞り込みあり
        } else if(hasCategory) {
            return foodRepository.findByUserIdAndCategory_CategoryName(userId, categoryName, sort);
        // 4. 絞り込みなし（ログインユーザーの食材全件）
        } else {
            return foodRepository.findByUserId(userId, sort);
        }
    }
}

