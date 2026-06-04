package com.example.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.entity.Food;

public interface FoodRepository extends JpaRepository<Food, Long>{
    
    List<Food> findByCategoryId(Long categoryId);
    List<Food> findByFoodNameContaining(String keyword);
}
