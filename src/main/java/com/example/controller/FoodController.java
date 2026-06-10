package com.example.controller;

import java.security.Principal; // ★ログインユーザー情報取得のために追加
import java.util.List;
import java.util.Set;//【追加】

import jakarta.validation.Valid; //【追加】

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult; //【追加】
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.entity.Category;
import com.example.entity.Food;
import com.example.entity.Unit;
import com.example.entity.User; // ★Userエンティティをインポート
import com.example.repository.FoodRepository;
import com.example.service.UnitService;
import com.example.service.UserService; // ★UserServiceをインポート

import reactor.core.publisher.Flux;

@Controller
@RequestMapping("/foods")
public class FoodController {

    @Autowired
    private com.example.service.FoodService foodService;

    @Autowired
    private com.example.service.CategoryService categoryService;

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UnitService unitService;

    // ★ Spring AIのOllamaクライアントを注入
    @Autowired
    private OllamaChatModel chatModel;

    //【追加】バリデーターの準備
    @Autowired
    private jakarta.validation.Validator beanValidator;

    // 一覧表示 (URL: GET /foods)
    @GetMapping
    public String index(
            @RequestParam(name = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String categoryNames,
            Model model,
            Principal principal) { // ★引数に Principal を追加

        // 1. ログイン中のユーザー情報を取得
        String username = principal.getName();
        User currentUser = userService.findByUsername(username);

        // 💡 修正：currentUser が null の場合のガード句を追加
        if (currentUser == null) {
            // ユーザーが見つからない場合は強制的にログアウトさせる、またはログイン画面へリダイレクト
            return "redirect:/login?error";
        }

        Long userId = currentUser.getId(); // これでNullPointerExceptionを絶対に防ぎます

        Sort sortOrder = direction.equalsIgnoreCase("desc") ? Sort.by(sort).descending() : Sort.by(sort).ascending();

        List<Category> allCategories = categoryService.getAllCategories();

        //重複しているカテゴリ名をまとめる（カテゴリ絞り込みのため）
        List<String> bundleCategoryNames = allCategories.stream()
                .map(Category::getCategoryName)
                .distinct()
                .toList();

        model.addAttribute("categoryNames", bundleCategoryNames);

        //htmlのth:selectedの判定用
        model.addAttribute("currentCategoryName", categoryNames);

        // 2. ログインユーザーのIDを検索条件に渡して、自分の食材だけを取得する
        // (※FoodService側に searchFoodsByUserId メソッドを追加する必要があります)
        List<Food> foods = foodService.searchFoodsByUserId(userId, keyword, categoryNames, sortOrder);

        model.addAttribute("foods", foods);

        model.addAttribute("currentKeyword", keyword);
        model.addAttribute("currentCategoryId", categoryId);
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentDirection", direction);
        model.addAttribute("reverseDirection", direction.equals("asc") ? "desc" : "asc");

        //【追加】usernameを得る
        model.addAttribute("loginUser", currentUser.getUsername());

        return "foods/index";
    }

    // 登録画面表示 (URL: GET /foods/new)
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("food", new Food());

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

    //登録処理
    @PostMapping
    public String store(
            @Valid @ModelAttribute Food food,
            BindingResult bindingResult,
            @RequestParam("categoryName") String categoryName,
            @RequestParam(value = "unit", required = false) String unit,
            Principal principal, Model model) {

        // 1. 手動チェック用のテンポラリオブジェクトを構築
        Category tempCategory = new Category();
        tempCategory.setCategoryName(categoryName);

        Unit tempUnit = new Unit();
        tempUnit.setUnitName(unit);
        tempCategory.setUnit(tempUnit);

        // 2. Category自体のバリデーション（カテゴリ名のチェック）
        Set<jakarta.validation.ConstraintViolation<Category>> categoryViolations = beanValidator.validate(tempCategory);
        for (jakarta.validation.ConstraintViolation<Category> violation : categoryViolations) {
            if (violation.getPropertyPath().toString().contains("categoryName")) {
                model.addAttribute("categoryNameError", violation.getMessage());
            }
        }

        // 3. Unit単体のバリデーション（単位のチェック）
        Set<jakarta.validation.ConstraintViolation<Unit>> unitViolations = beanValidator.validate(tempUnit);
        for (jakarta.validation.ConstraintViolation<Unit> violation : unitViolations) {
            if (violation.getPropertyPath().toString().contains("unitName")) {
                model.addAttribute("unitError", violation.getMessage());
            }
        }

        // 4. エラー判定：食材名(Food)・カテゴリ・単位のいずれかに不備があれば画面に戻す
        if (bindingResult.hasErrors() || model.containsAttribute("categoryNameError")
                || model.containsAttribute("unitError")) {
            List<Category> categories = categoryService.getAllCategories();
            model.addAttribute("allCategories", categories);
            model.addAttribute("currentCategoryName", categoryName);
            model.addAttribute("currentUnit", unit);
            return "foods/create";
        }

        // 5. 正常処理
        String username = principal.getName();
        User currentUser = userService.findByUsername(username);
        food.setUser(currentUser);

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

        List<Category> categories = categoryService.getAllCategories();

        for (Category c : categories) {
            if (c.getUnit() == null) {
                Unit dummyUnit = new Unit();
                dummyUnit.setUnitName("個"); // デフォルトの単位
                c.setUnit(dummyUnit);
            }
        }

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

    @PostMapping("/{id}/update")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute Food food,
            BindingResult bindingResult,
            @RequestParam("categoryName") String categoryName,
            @RequestParam(value = "unit", required = false) String unit,
            Principal principal,
            Model model) {

        // 1. 手動チェック用のテンポラリオブジェクトを構築
        Category tempCategory = new Category();
        tempCategory.setCategoryName(categoryName);

        Unit tempUnit = new Unit();
        tempUnit.setUnitName(unit);
        tempCategory.setUnit(tempUnit);

        // 2. カテゴリ名のバリデーション
        Set<jakarta.validation.ConstraintViolation<Category>> categoryViolations = beanValidator.validate(tempCategory);
        for (jakarta.validation.ConstraintViolation<Category> violation : categoryViolations) {
            if (violation.getPropertyPath().toString().contains("categoryName")) {
                model.addAttribute("categoryNameError", violation.getMessage());
            }
        }

        // 3. 単位のバリデーション
        Set<jakarta.validation.ConstraintViolation<Unit>> unitViolations = beanValidator.validate(tempUnit);
        for (jakarta.validation.ConstraintViolation<Unit> violation : unitViolations) {
            if (violation.getPropertyPath().toString().contains("unitName")) {
                model.addAttribute("unitError", violation.getMessage());
            }
        }

        // 4. エラー判定：既存の入力値を保持して編集画面へ戻す
        if (bindingResult.hasErrors() || model.containsAttribute("categoryNameError")
                || model.containsAttribute("unitError")) {
            List<Category> categories = categoryService.getAllCategories();
            model.addAttribute("allCategories", categories);

            // 画面の入力値を上書き維持
            model.addAttribute("currentCategoryName", categoryName);
            model.addAttribute("currentUnit", unit);

            // 編集画面に必要な最大文字数の再計算
            int maxFoodNameLength = 10;
            try {
                maxFoodNameLength = Food.class.getDeclaredField("foodName")
                        .getAnnotation(jakarta.validation.constraints.Size.class).max();
            } catch (Exception e) {
            }
            model.addAttribute("maxFoodNameLength", maxFoodNameLength);

            return "foods/edit";
        }

        // 5. 正常時の更新処理
        Food existingFood = foodService.getFoodById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid food Id:" + id));

        existingFood.setFoodName(food.getFoodName());
        existingFood.setAmount(food.getAmount());
        existingFood.setTasteLimit(food.getTasteLimit());

        String username = principal.getName();
        User currentUser = userService.findByUsername(username);
        existingFood.setUser(currentUser);

        handleCategory(existingFood, categoryName, unit);
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
        newAmount = Math.round(newAmount * 100.0) / 100.0;

        if (newAmount > 0) {
            // 残量がある場合は、数量を更新して保存
            food.setAmount(newAmount);
            foodService.saveFood(food);
        } else {
            foodService.deleteFood(id);
        }

        return "redirect:/foods";
    }

    private void handleCategory(Food food, String categoryName, String unitStr) {
        if (categoryName == null || categoryName.strip().isEmpty()) {
            return;
        }
        String trimmedCategoryName = categoryName.strip();

        Unit targetUnit = unitService.getOrCreateUnit(unitStr);

        List<Category> allCategories = categoryService.getAllCategories();

        Category targetCategory = null;

        for (Category kizonCategory : allCategories) {
            if (kizonCategory.getCategoryName().equals(trimmedCategoryName) &&
                    kizonCategory.getUnit().getId().equals(targetUnit.getId())) {

                targetCategory = kizonCategory;
                break;
            }
        }

        if (targetCategory == null) {
            Category newCategory = new Category();
            newCategory.setCategoryName(trimmedCategoryName);
            newCategory.setUnit(targetUnit);

            targetCategory = categoryService.saveCategory(newCategory);

        }
        food.setCategory(targetCategory);
    }

    /**
     * 1. ユーザーがボタンを押したとき、一瞬でローディング画面を開く処理
     * URL: GET /foods/{id}/recipe
     */
    @GetMapping("/{id}/recipe")
    public String recipePage(@PathVariable Long id, Model model) {
        Food food = foodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid food Id:" + id));
        // 単一のfoodオブジェクトを「food」という名前で画面に渡す
        model.addAttribute("food", food);
        return "foods/recipe";
    }

    /**
     * 2. 選択された1つの食材のみを考慮して、文字をストリーミング出力するAPI
     * URL: GET /foods/{id}/recipe/generate
     */
    @GetMapping(value = "/{id}/recipe/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public Flux<String> generateRecipeStream(@PathVariable Long id) {
        Food mainFood = foodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid food Id: " + id));

        String prompt = String.format(
                "あなたは親切なプロの料理人です。食材「%s」と一般的な調味料を使い、" +
                        "家庭で簡単に作れる美味しい料理のレシピを1つ提案してください。\n\n" +
                        "以下の構成で日本語で出力してください：\n" +
                        "1. 料理名\n" +
                        "2. 材料（分量）\n" +
                        "3. 作り方の手順\n" +
                        "4. 美味しく作るためのコツ",
                mainFood.getFoodName());

        // 末尾に [DONE] フラグを付与して、安全にフロントに終了を伝える
        return chatModel.stream(prompt)
                .concatWith(Flux.just("\n[DONE]"));
    }

}
