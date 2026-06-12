package com.example.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.entity.ShoppingMemo;

public interface ShoppingMemoRepository extends JpaRepository<ShoppingMemo, Long> {
    // ログインユーザーのIDに紐づくメモ一覧を、登録が新しい順（降順）で取得
    List<ShoppingMemo> findByUserIdOrderByCreatedAtDesc(Long userId);
}