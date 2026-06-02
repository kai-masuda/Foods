package com.example.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.entity.Category;
import com.example.entity.Food;

@Controller
@RequestMapping("/ingredients")
public class FoodController {
    
    @Autowired
    private com.example.service.FoodService foodService;
    
    @Autowired
    private com.example.service.CategoryService categoryService;
    
    //一覧表示
    @GetMapping
    public String index(Model model) {
        List<Food> foods = foodService.getAllFoods();
        model.addAttribute("foods", foods);
        return "ingredients/index";
    }
    
    //登録画面表示
    @GetMapping
    public String createForm(Model model) {
        model.addAttribute("food", new Food());
        
        //
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);
        
        return "ingredients/create";
    }
    
    //登録実行
    @PostMapping
    public String store(@ModelAttribute Food food) {
        foodService.saveFood(food); // データベースに保存
        return "redirect:/ingredients"; // 保存が終わったら一覧画面に自動で戻る
    }
    
    // 4. 編集画面の表示
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Optional<Food> food = foodService.getFoodById(id); // 編集したい食品データを1件取得
        model.addAttribute("food", food);
        
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);
        
        return "ingredients/edit";
    }
    
    // 5. 更新の実行 (Update)
    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id, @ModelAttribute Food food) {
        food.setId(id); // 安全のため、URLのIDをセットする
        foodService.saveFood(food); // データを上書き保存
        return "redirect:/ingredients";
    }
    
    // 6. 削除の実行 (Delete)
    @PostMapping("/{id}/delete") // 先ほど登場した destroy 処理です！
    public String destroy(@PathVariable Long id) {
        foodService.deleteFood(id); // データを削除
        return "redirect:/ingredients";
    }

}
