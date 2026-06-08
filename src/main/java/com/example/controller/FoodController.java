package com.example.controller; // 実際のパッケージ名に合わせてください

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // 💡【重要】Modelを解決するために必須のインポート
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.entity.Category; // 実際のエンティティのパッケージ名に合わせてください
import com.example.entity.Food;
import com.example.repository.FoodRepository;

@Controller
@RequestMapping("/foods")
public class FoodController {

    @Autowired
    private com.example.service.FoodService foodService;

    @Autowired
    private com.example.service.CategoryService categoryService;
    
    @Autowired
    private FoodRepository foodRepository;

    // 一覧表示 (URL: GET /foods)
    @GetMapping
    public String index(
            @RequestParam(name = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) Long categoryId,
            Model model) {

        Sort sortOrder = direction.equalsIgnoreCase("desc") ? Sort.by(sort).descending() : Sort.by(sort).ascending();

        List<Food> foods = foodService.serchFoods(keyword, categoryId, sortOrder);

        model.addAttribute("foods", foods);
        model.addAttribute("currentKeyword", keyword);
        model.addAttribute("currentCategoryId", categoryId);

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

        // 💡【修正】HTMLの <option th:each="c : ${allCategories}"> と名前を一致させるため、
        // 属性名を "categories" から "allCategories" に変更しました
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("allCategories", categories);

        int maxFoodNameLength = 10;
        try {
            maxFoodNameLength = Food.class
                    .getDeclaredField("foodName")
                    .getAnnotation(jakarta.validation.constraints.Size.class)
                    .max();
        } catch (Exception e) {
            // 失敗時はデフォルト維持
        }
        model.addAttribute("maxFoodNameLength", maxFoodNameLength);

        return "foods/create";
    }

    // 登録実行 (URL: POST /foods)
    @PostMapping
    public String store(
            @ModelAttribute Food food,
            // 💡【修正】HTMLの各inputの `name="categoryName"` および `name="unit"` と名前を一致させました
            @RequestParam("categoryName") String categoryName,
            @RequestParam(value = "unit", required = false) String unit) {

        // 修正した引数で handleCategory を呼び出し
        handleCategory(food, categoryName, unit);
        foodService.saveFood(food);
        return "redirect:/foods";
    }

    // 4. 編集画面の表示 (URL: GET /foods/{id}/edit)
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Food food = foodService.getFoodById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid food Id:" + id));
        model.addAttribute("food", food);

        // 💡 編集画面でも datalist を使う場合は、登録画面と同様に "allCategories" に変更しておくと安全です
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("allCategories", categories);

        int maxFoodNameLength = 10;
        try {
            maxFoodNameLength = Food.class
                    .getDeclaredField("foodName")
                    .getAnnotation(jakarta.validation.constraints.Size.class)
                    .max();
        } catch (Exception e) {
            // 失敗時はデフォルト維持
        }
        model.addAttribute("maxFoodNameLength", maxFoodNameLength);

        return "foods/edit";
    }

    // 5. 更新の実行 (URL: POST /foods/{id}/update)
    @PostMapping("/{id}/update")
    public String update(
            @PathVariable Long id,
            @ModelAttribute Food food,
            // 💡【修正】画面のinput名 (categoryName, unit) と一致させて文字列で受け取る
            @RequestParam("categoryName") String categoryName,
            @RequestParam(value = "unit", required = false) String unit) {

        // 1. データベースから古い食材データを取得
        Food existingFood = foodService.getFoodById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid food Id:" + id));

        // 2. 画面から入力された値を古いデータに上書き
        existingFood.setFoodName(food.getFoodName());
        existingFood.setAmount(food.getAmount());
        existingFood.setTasteLimit(food.getTasteLimit());

        // 3. 編集されたカテゴリ名と単位を元に、handleCategoryメソッドで適切に解決・紐付け
        handleCategory(existingFood, categoryName, unit);

        // 4. データベースに上書き保存
        foodService.saveFood(existingFood);
        return "redirect:/foods";
    }

    // 6. 削除・消費の実行 (URL: POST /foods/{id}/delete)
    @PostMapping("/{id}/delete")
    public String destroy(
            @PathVariable Long id,
            @RequestParam("reduceAmount") double reduceAmount) {

        // 1. 対象の食材データをデータベースから取得
        Food food = foodService.getFoodById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid food Id:" + id));

        // 2. 現在の数量から、消費する数量を引き算する
        double newAmount = food.getAmount() - reduceAmount;

        if (newAmount > 0) {
            // 残量がある場合は、数量を更新して保存
            food.setAmount(newAmount);
            foodService.saveFood(food);
        } else {
            foodService.deleteFood(id);
        }

        return "redirect:/foods";
    }

    private void handleCategory(Food food, String categoryName, String unit) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return;
        }
        String trimmedName = categoryName.trim();

        List<Category> allCategories = categoryService.getAllCategories();

        Category targetCategory = null;
        String targetUnit = (unit != null) ? unit.trim() : "";

        if (targetUnit.isEmpty()) {
            // 単位が空欄なら、同じ名前の既存カテゴリを検索
            targetCategory = allCategories.stream()
                    .filter(c -> c.getCategoryName().equals(trimmedName))
                    .findFirst()
                    .orElse(null);

            if (targetCategory != null) {
                // 既存のカテゴリが見つかったので、その単位を引き継ぐ
                targetUnit = targetCategory.getUnit();
            } else {
                // 完全新規で単位も空ならデフォルトの「個」
                targetUnit = "個";
            }
        } else {
            // 単位が入力されている場合は、名前と単位の両方が一致するものを探す
            String finalUnit = targetUnit;
            targetCategory = allCategories.stream()
                    .filter(c -> c.getCategoryName().equals(trimmedName) && finalUnit.equals(c.getUnit()))
                    .findFirst()
                    .orElse(null);
        }

        // 一致するものがなければ、新しいカテゴリとして保存
        if (targetCategory == null) {
            Category newCategory = new Category();
            newCategory.setCategoryName(trimmedName);
            newCategory.setUnit(targetUnit);

            targetCategory = categoryService.saveCategory(newCategory);
        }

        // 確定したカテゴリをFoodに紐付ける
        food.setCategory(targetCategory);
    }
    
    @GetMapping("/{id}/recipe")
    public String recipePage(@PathVariable Long id, Model model) {
        
        Food food = foodRepository.findById(id).orElseThrow();
        model.addAttribute("food", food);
        model.addAttribute("recipe", "");
        return "foods/recipe";
    }
}
