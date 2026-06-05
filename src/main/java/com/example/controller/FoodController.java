package com.example.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.entity.Category;
import com.example.entity.Food;

@Controller
@RequestMapping("/foods")
public class FoodController {
    
    @Autowired
    private com.example.service.FoodService foodService;
    
    @Autowired
    private com.example.service.CategoryService categoryService;
    
    // 一覧表示 (URL: GET /foods)
    @GetMapping
    public String index(
        // ◆追加：検索キーワード（最初はなくてもいいように、 required = false）を受け取る
        //【修正】デフォルトでnullになるように変更
        @RequestParam(name = "keyword", required = false, defaultValue = "") String keyword,
        // デフォルトは賞味期限順に設定。sort: 列の名前を保存するための変数
        @RequestParam(defaultValue = "id") String sort,
        // デフォルトは昇順に設定。direction: 並び替えの方向を保存するための変数
        @RequestParam(defaultValue = "asc") String direction,
        //◆追加：絞り込み（カテゴリID）
        @RequestParam(required = false) Long categoryId,
        Model model) {

        
        Sort sortOrder = direction.equalsIgnoreCase("desc") ?
                Sort.by(sort).descending() : Sort.by(sort).ascending();
        
        //一覧表示・絞り込み・検索機能統合メソッド呼び出し
        List<Food> foods = foodService.serchFoods(keyword, categoryId, sortOrder);
        
        model.addAttribute("foods", foods);
        
        //HTML側で、今何が選択されているかを維持・表示する
        model.addAttribute("currentKeyword", keyword);
        model.addAttribute("currentCategoryId", categoryId);
        
        //カテゴリを全権取得
        model.addAttribute("categories", categoryService.getAllCategories());
        
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentDirection", direction);
        model.addAttribute("reverseDirection", direction.equals("asc") ? "desc" : "asc");
        
        return "foods/index";
    }
    
    // 登録画面表示 (URL: GET /foods/new)
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("food", new Food());
        
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);
        
        int maxFoodNameLength = 10; // デフォルト値
        try {
            maxFoodNameLength = Food.class
                    .getDeclaredField("foodName")
                    .getAnnotation(jakarta.validation.constraints.Size.class)
                    .max();
        } catch (Exception e) {
            // 万が一取得失敗した場合はデフォルトの10を維持
        }
        model.addAttribute("maxFoodNameLength", maxFoodNameLength);
 
        
        return "foods/create";
    }
    
    // 登録実行 (URL: POST /foods)
    @PostMapping
    public String store(
            @ModelAttribute Food food, 
            @RequestParam("categoryInputName") String categoryInputName,
            @RequestParam(value = "newCategoryUnit", required = false) String newCategoryUnit) {
        
        // メソッド名を「handleCategory」に統一して呼び出す
        handleCategory(food, categoryInputName, newCategoryUnit);
        foodService.saveFood(food); 
        return "redirect:/foods";
    }
    
    // 4. 編集画面の表示 (URL: GET /foods/{id}/edit)
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Food food = foodService.getFoodById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid food Id:" + id));
        model.addAttribute("food", food);
        
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);
        
        int maxFoodNameLength = 10; // デフォルト値
        try {
            maxFoodNameLength = Food.class
                    .getDeclaredField("foodName")
                    .getAnnotation(jakarta.validation.constraints.Size.class)
                    .max();
        } catch (Exception e) {
            // 万が一取得失敗した場合はデフォルトの10を維持
        }
        model.addAttribute("maxFoodNameLength", maxFoodNameLength);
 
        
        return "foods/edit";
    }
    
    // 5. 更新の実行 (URL: POST /foods/{id}/update)
    @PostMapping("/{id}/update")
    public String update(
            @PathVariable Long id, 
            @ModelAttribute Food food, 
            // 💡 【修正】@RequestParam("category") で、セレクトボックスから送信されるカテゴリIDを「String型」として受け取ります。
            @RequestParam("category") String categoryIdStr,
            @RequestParam(value = "newCategoryUnit", required = false) String newCategoryUnit) {
        
        // 💡 1. データベースから「現在保存されている古い食材データ」を確実に1件取得
        Food existingFood = foodService.getFoodById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid food Id:" + id));

        // 💡 2. 画面から入力された新しい値（名前・数量・期限）を、取得した古いデータに上書き
        existingFood.setFoodName(food.getFoodName());
        existingFood.setAmount(food.getAmount());
        existingFood.setTasteLimit(food.getTasteLimit()); 

        // 💡 3. セレクトボックスから送られてきたのはIDなので、データベースから対応する「カテゴリ名」を探し出す
        String categoryName = "";
        try {
            Long selectedCategoryId = Long.parseLong(categoryIdStr);
            Category selectedCategory = categoryService.getAllCategories().stream()
                    .filter(c -> c.getId().equals(selectedCategoryId))
                    .findFirst()
                    .orElse(null);
            
            if (selectedCategory != null) {
                categoryName = selectedCategory.getCategoryName(); // 名前を抽出！
            }
        } catch (NumberFormatException e) {
            // 万が一数値変換に失敗した（文字列が直接送られてきた）場合は、そのまま文字列を使用
            categoryName = categoryIdStr;
        }

        // 💡 4. 抽出した「カテゴリ名」を使って、既存の handleCategory メソッドに処理を流す
        handleCategory(existingFood, categoryName, newCategoryUnit);
        
        // 💡 5. データベースに安全に上書き保存
        foodService.saveFood(existingFood); 
        return "redirect:/foods";
    }

    
    // 6. 削除・消費の実行 (URL: POST /foods/{id}/delete)
    @PostMapping("/{id}/delete") 
    public String destroy(
            @PathVariable Long id, 
            //【修正】reduceAmountをdouble型に変更
            @RequestParam("reduceAmount") double reduceAmount) { // ◆追加：画面から消費する数量を受け取る
        
        // 1. 対象の食材データをデータベースから取得
        Food food = foodService.getFoodById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid food Id:" + id));
        
        // 2. 現在の数量から、消費する数量を引き算する
        //【修正】newAmountをdouble型に変更
        double newAmount = food.getAmount() - reduceAmount;
        
        if (newAmount > 0) {
            // 残量がある場合は、数量を更新して保存
            food.setAmount(newAmount);
            foodService.saveFood(food);
        } else {
            // 0以下になる場合は、データそのものをデータベースから削除
            foodService.deleteFood(id); 
        }
        
        return "redirect:/foods";
    }


    //カテゴリの追加
    private void handleCategory(Food food, String categoryName, String unit) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return;
        }
        String trimmedName = categoryName.trim();

        // 既存の全カテゴリを取得して、名前が一致するデータがあるか探す
        List<Category> allCategories = categoryService.getAllCategories();
        Category targetCategory = allCategories.stream()
                .filter(c -> c.getCategoryName().equals(trimmedName))
                .findFirst()
                .orElse(null);

        // 一致するものがなければ、新しいカテゴリとしてデータベースに保存
        if (targetCategory == null) {
            Category newCategory = new Category();
            newCategory.setCategoryName(trimmedName);
            
            if (unit != null && !unit.trim().isEmpty()) {
                newCategory.setUnit(unit.trim());
            } else {
                newCategory.setUnit("個");
            }
            
            targetCategory = categoryService.saveCategory(newCategory);
        }

        // 確定したカテゴリをFoodに紐付ける
        food.setCategory(targetCategory);
    }

}


