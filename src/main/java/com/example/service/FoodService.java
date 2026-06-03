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
    
 // 食材の一覧取得
    public List<Food> getAllFoods() {
        return foodRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
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
}
