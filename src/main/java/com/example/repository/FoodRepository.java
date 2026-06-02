package com.example.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.entity.Food;

public interface FoodRepository extends JpaRepository<Food, Long>{
    Optional<Food> findByFoodName(String foodName);
    boolean existsByFoodName(String foodName);

}
