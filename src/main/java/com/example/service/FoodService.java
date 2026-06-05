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
    
    // 食材の一覧取得  //【修正】引数sort追加
    public List<Food> getAllFoods(Sort sort) {
        return foodRepository.findAll(sort);
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
    
    //検索・絞り込み統合
    public List<Food> serchFoods(String keyword, Long categoryId, Sort sort){
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasCategory = categoryId != null;
        
        //キーワード、カテゴリ絞り込みあり
        if(hasKeyword && hasCategory) {
            return foodRepository.searchByNameAndCategoryId(keyword, categoryId, sort);
        //キーワードあり
        } else if(hasKeyword){
            return foodRepository.searchByName(keyword, sort);
        //カテゴリ絞り込みあり
        } else if(hasCategory) {
            return foodRepository.findByCategoryId(categoryId, sort);
        //絞り込みなし
        } else {
            return foodRepository.findAll(sort);
        }
    }
}
