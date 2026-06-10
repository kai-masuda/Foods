package com.example.controller;

import java.security.Principal; // ★ログインユーザー情報取得のために追加
import java.util.List;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
                .map(Category :: getCategoryName)
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

    // 登録実行 (URL: POST /foods)
    @PostMapping
    public String store(
            @ModelAttribute Food food,
            @RequestParam("categoryName") String categoryName,
            @RequestParam(value = "unit", required = false) String unit,
            Principal principal) { // ★引数に Principal を追加

        // 1. ログイン中のユーザー情報を取得してFoodに紐付ける
        String username = principal.getName();
        User currentUser = userService.findByUsername(username);
        food.setUser(currentUser);

        // 2. カテゴリを処理して保存
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

    // 5. 更新の実行 (URL: POST /foods/{id}/update)
    @PostMapping("/{id}/update")
    public String update(
            @PathVariable Long id,
            @ModelAttribute Food food,
            @RequestParam("categoryName") String categoryName,
            @RequestParam(value = "unit", required = false) String unit,
            Principal principal) { // ★引数に Principal を追加

        // 1. データベースから古い食材データを取得
        Food existingFood = foodService.getFoodById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid food Id:" + id));

        // 2. 画面から入力された値を古いデータに上書き
        existingFood.setFoodName(food.getFoodName());
        existingFood.setAmount(food.getAmount());
        existingFood.setTasteLimit(food.getTasteLimit());
        existingFood.setCategory(food.getCategory());

        // 3. ログイン中のユーザー情報を取得して再セット（セキュリティ担保のため）
        String username = principal.getName();
        User currentUser = userService.findByUsername(username);
        existingFood.setUser(currentUser);

        // 4. 編集されたカテゴリ名と単位を元に、handleCategoryメソッドで適切に解決・紐付け
        handleCategory(existingFood, categoryName, unit);

        // 5. データベースに上書き保存
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
     * 1. ユーザーがボタンを押したら、まずは一瞬でローディング画面を開く処理
     * URL: GET /foods/{id}/recipe
     */
    @GetMapping("/{id}/recipe")
    public String recipePage(@PathVariable Long id, Model model) {
        // 食材情報をリポジトリ（またはfoodService）から取得して画面に渡す
        Food food = foodRepository.findById(id).orElseThrow();
        model.addAttribute("food", food);

        // フォルダ構造に合わせて templates/foods/recipe.html を呼び出す
        return "foods/recipe";
    }

    /**
     * 2. ★修正：他の所持食材も考慮して、文字をどんどんストリーミング出力するAPI
     * produces = MediaType.TEXT_EVENT_STREAM_VALUE を指定してSSE通信にします
     */
    @GetMapping(value = "/{id}/recipe/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public Flux<String> generateRecipeStream(@PathVariable Long id, Principal principal) { // ★ 引数に Principal を追加
        // 1. メインとなる選択された食材を取得
        Food mainFood = foodRepository.findById(id).orElseThrow();

        // 2. ログインユーザーの他の食材リストをテキスト化する
        String otherFoodsText = "なし";
        if (principal != null) {
            String username = principal.getName();
            User currentUser = userService.findByUsername(username);
            if (currentUser != null) {
                // ユーザーIDに紐づく食材リストをすべて取得
                List<Food> allFoods = foodRepository.findByUserId(currentUser.getId(), org.springframework.data.domain.Sort.unsorted());

                // メイン食材以外の名前をカンマ区切りで抽出
                List<String> otherFoodNames = allFoods.stream()
                        .map(Food::getFoodName)
                        .filter(name -> !name.equals(mainFood.getFoodName()))
                        .toList();

                if (!otherFoodNames.isEmpty()) {
                    otherFoodsText = String.join("、", otherFoodNames);
                }
            }
        }

        // 3. 他の食材も利用するようにプロンプトを構築
        String prompt = String.format(
                "あなたは親切なプロの料理人です。メイン食材「%s」を使い、さらに可能であれば冷蔵庫にある他の食材「%s」も有効活用した、" +
                        "家庭で簡単に作れる美味しい料理のレシピを1つ提案してください。また、冷蔵庫にないもので追加で買う必要のあるものはレシピの最後に書いといて下さい。\n" +
                        "（他の食材はすべてを使う必要はありません。相性の良いものを組み合わせてください）\n\n" +
                        "以下の構成で日本語で出力してください：\n" +
                        "1. 料理名\n" +
                        "2. 材料（分量）\n" +
                        "3. 作り方の手順（ステップバイステップで分かりやすく）\n" +
                        "4. 美味しく作るためのコツ \n" +
                        "ただし、以下のルールを守ってください \n" +
                        "・食べ物ではないモノが指定された際には、「食べ物以外は受け付けません」という旨の文章を返し、レシピは表示しない。 \n" +
                        "・一般的に食べ物とされていない動物や植物（ライオンやカメ、虫など）が指定された際には、「一般的に食用とされていないものは受け付けません」という旨の文章を返し、レシピは表示しない。\n" +
                        "・必ず日本語を使用する。",
                mainFood.getFoodName(),
                otherFoodsText);

        // ★ .call() ではなく .stream() を使うことで、文字が生成されるたびに順次JavaScriptに送信されます
        return chatModel.stream(prompt);
    }

}
