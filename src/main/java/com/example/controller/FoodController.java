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
            @RequestParam(defaultValue = "taste_limit") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {
        
        Sort sortOrder = direction.equalsIgnoreCase("desc") ?
                Sort.by(sort).descending() : Sort.by(sort).ascending();
        
        List<Food> foods = foodService.getAllFoods(sortOrder);
        model.addAttribute("foods", foods);
        
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
        
        return "foods/edit";
    }
    
    // 5. 更新の実行 (URL: POST /foods/{id}/update)
    @PostMapping("/{id}/update")
    public String update(
            @PathVariable Long id, 
            @ModelAttribute Food food, 
            @RequestParam("categoryInputName") String categoryInputName,
            @RequestParam(value = "newCategoryUnit", required = false) String newCategoryUnit) {
        
        food.setId(id); 
        // 【修正】ここも「handleCategory」に名前を統一して呼び出す
        handleCategory(food, categoryInputName, newCategoryUnit);
        foodService.saveFood(food); 
        return "redirect:/foods";
    }
    
    // 6. 削除の実行 (URL: POST /foods/{id}/delete)
    @PostMapping("/{id}/delete") 
    public String destroy(@PathVariable Long id) {
        foodService.deleteFood(id); 
        return "redirect:/foods";
    }

    // 【名称変更】呼び出し元と名前を完全に一致させました
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


