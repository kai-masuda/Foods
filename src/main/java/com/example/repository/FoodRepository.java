package com.example.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // ★追加
import org.springframework.data.repository.query.Param; // ★追加

import com.example.entity.Food;

public interface FoodRepository extends JpaRepository<Food, Long>{
    
    List<Food> findByCategoryId(Long categoryId, Sort sort);
    @Query("SELECT f FROM Food f WHERE f.foodName LIKE %:keyword%")
    List<Food> searchByName(@Param("keyword") String keyword, Sort sort);
    @Query("SELECT f FROM Food f WHERE f.foodName LIKE %:keyword% AND f.category.id = :categoryId")
    List<Food> searchByNameAndCategoryId(@Param("keyword") String keyword, @Param("categoryId") Long categoryId, Sort sort);
}
