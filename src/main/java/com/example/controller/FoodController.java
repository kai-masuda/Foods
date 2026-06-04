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
    
    // 一覧表示 (URL: GET /foods)  // 【修正】パラメータを受け取れるように変更
    @GetMapping
    public String index(
            // デフォルトは賞味期限順に設定。sort: 列の名前を保存するための変数
            @RequestParam(defaultValue = "taste_limit") String sort,
            // デフォルトは昇順に設定。direction: 並び替えの方向を保存するための変数
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {
        
        // ソートオブジェクトの組み立て
        // IgnoreCase: 大文字と小文字の違いを無視
        Sort sortOrder = direction.equalsIgnoreCase("desc") ?
                Sort.by(sort).descending() : Sort.by(sort).ascending();
        
        // 引数にソート条件を渡して、並び替え済みのデータを取得する
        List<Food> foods = foodService.getAllFoods(sortOrder);
        model.addAttribute("foods", foods);
        
        // Thymeleafで並び替えリンクや上下表示を制御するための情報を渡す
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
        
        return "foods/create";
    }
    
    // 登録実行 (URL: POST /foods)
    @PostMapping
    public String store(@ModelAttribute Food food) {
        foodService.saveFood(food); 
        return "redirect:/foods";
    }
    
    // 4. 編集画面の表示 (URL: GET /foods/{id}/edit)
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        // 【修正】OptionalからFoodオブジェクトを安全に取り出す記述に変更しました
        Food food = foodService.getFoodById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid food Id:" + id));
        model.addAttribute("food", food);
        
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);
        
        return "foods/edit";
    }
    
    // 5. 更新の実行 (URL: POST /foods/{id}/update)
    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id, @ModelAttribute Food food) {
        food.setId(id); 
        foodService.saveFood(food); 
        return "redirect:/foods";
    }
    
    // 6. 削除の実行 (URL: POST /foods/{id}/delete)
    @PostMapping("/{id}/delete") 
    public String destroy(@PathVariable Long id) {
        foodService.deleteFood(id); 
        return "redirect:/foods";
    }

}

