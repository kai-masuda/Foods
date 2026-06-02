package com.example.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.entity.Category;
import com.example.service.CategoryService;

@RestController // ★ポイント：HTML画面ではなく「データ（JSON）」を返すコントローラー
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    // カテゴリ一覧をJSON形式で返す（JavaScriptなどで非同期に取得したい場合などに便利）
    @GetMapping
    public List<Category> getAllCategories() {
        return categoryService.getAllCategories();
    }
}
