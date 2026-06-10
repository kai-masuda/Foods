package com.example.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.entity.Food;

public interface FoodRepository extends JpaRepository<Food, Long> {

    // 💡【統合修正】引数に Sort を持つこちら1つだけに統一します。
    // ソートなしで全件取得したい場合は、呼び出し側で `Sort.unsorted()` を渡せば正常に動作します。
    List<Food> findByUserId(Long userId, Sort sort);

    // ★特定のユーザー、かつ特定のカテゴリで絞り込む
    List<Food> findByUserIdAndCategory_CategoryName(Long userId, String categoryName, Sort sort);

    // ★特定のユーザー、かつキーワードで部分一致検索する
    @Query("SELECT f FROM Food f WHERE f.user.id = :userId AND f.foodName LIKE %:keyword%")
    List<Food> searchByUserIdAndName(@Param("userId") Long userId, @Param("keyword") String keyword, Sort sort);

    // ★特定のユーザー、かつキーワード、かつカテゴリ名で絞り込む
    @Query("SELECT f FROM Food f WHERE f.user.id = :userId AND f.foodName LIKE %:keyword% AND f.category.categoryName = :categoryName")
    List<Food> searchByUserIdAndNameAndCategoryName(@Param("userId") Long userId, @Param("keyword") String keyword, @Param("categoryName") String categoryName, Sort sort);
    
    // 💡【修正】@Queryアノテーションが抜けていたため、正しくJPQLを記述しました
    @Query("SELECT f FROM Food f WHERE f.user.id = :userId AND f.foodName LIKE %:keyword% AND f.category.id = :categoryId")
    List<Food> searchByUserIdAndNameAndCategoryId(@Param("userId") Long userId, @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId, Sort sort);
}
