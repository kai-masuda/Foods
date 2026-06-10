package com.example.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.entity.Food;

public interface FoodRepository extends JpaRepository<Food, Long> {

    // ★追加：特定のユーザーの食材を全件取得する
    List<Food> findByUserId(Long userId, Sort sort);

    // ★修正：特定のユーザー、かつ特定のカテゴリで絞り込む
    List<Food> findByUserIdAndCategoryId(Long userId, Long categoryId, Sort sort);

    // ★修正：特定のユーザー、かつキーワードで部分一致検索する
    @Query("SELECT f FROM Food f WHERE f.user.id = :userId AND f.foodName LIKE %:keyword%")
    List<Food> searchByUserIdAndName(@Param("userId") Long userId, @Param("keyword") String keyword, Sort sort);

    // ★修正：特定のユーザー、かつキーワード、かつカテゴリで絞り込む
    @Query("SELECT f FROM Food f WHERE f.user.id = :userId AND f.foodName LIKE %:keyword% AND f.category.id = :categoryId")
    List<Food> searchByUserIdAndNameAndCategoryId(@Param("userId") Long userId, @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId, Sort sort);

    // 💡 ユーザーIDに紐づく食材リストを取得するメソッドを追加
    List<Food> findByUserId(Long userId);
}
