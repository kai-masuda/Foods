package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Ingredient;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    // 期限順（昇順）で全件取得
    List<Ingredient> findAllByOrderByExpiryDateAsc();
    
    // 食材名でのあいまい検索＋期限順ソート
    List<Ingredient> findByNameContainingOrderByExpiryDateAsc(String name);
}
