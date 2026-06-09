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
import com.example.entity.User; // ★Userエンティティをインポート
import com.example.repository.FoodRepository;
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
    private UserService userService; // ★ログインユーザーを取得するために追加
    
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
            Model model,
            Principal principal) { // ★引数に Principal を追加

        // 1. ログイン中のユーザー情報を取得
        String username = principal.getName();
        User currentUser = userService.findByUsername(username);
        Long userId = currentUser.getId();

        Sort sortOrder = direction.equalsIgnoreCase("desc") ? Sort.by(sort).descending() : Sort.by(sort).ascending();

        // 2. ログインユーザーのIDを検索条件に渡して、自分の食材だけを取得する
        // (※FoodService側に searchFoodsByUserId メソッドを追加する必要があります)
        List<Food> foods = foodService.searchFoodsByUserId(userId, keyword, categoryId, sortOrder);

        model.addAttribute("foods", foods);
        model.addAttribute("currentKeyword", keyword);
        model.addAttribute("currentCategoryId", categoryId);

        model.addAttribute("categories", categoryService.getAllCategories());

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
            targetCategory = allCategories.stream()
                    .filter(c -> c.getCategoryName().equals(trimmedName))
                    .findFirst()
                    .orElse(null);

            if (targetCategory != null) {
                targetUnit = targetCategory.getUnit();
            } else {
                targetUnit = "個";
            }
        } else {
            String finalUnit = targetUnit;
            targetCategory = allCategories.stream()
                    .filter(c -> c.getCategoryName().equals(trimmedName) && finalUnit.equals(c.getUnit()))
                    .findFirst()
                    .orElse(null);
        }

        if (targetCategory == null) {
            Category newCategory = new Category();
            newCategory.setCategoryName(trimmedName);
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
     * 2. ★修正：文字をどんどんストリーミング出力するAPI
     * produces = MediaType.TEXT_EVENT_STREAM_VALUE を指定してSSE通信にします
     */
    @GetMapping(value = "/{id}/recipe/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public Flux<String> generateRecipeStream(@PathVariable Long id) {
        Food food = foodRepository.findById(id).orElseThrow();

        String prompt = String.format(
            "あなたは親切なプロの料理人です。食材「%s」を使った、家庭で簡単に作れる美味しい料理のレシピを3つ提案してください。\n\n" +
            "以下の構成で日本語で出力してください：\n" +
            "1. 料理名\n" +
            "2. 材料（分量）\n" +
            "3. 作り方の手順（ステップバイステップで分かりやすく）\n" +
            "4. 美味しく作るためのコツ \n" +
            "ただし、以下のルールを守ってください \n" + 
            "・食べ物ではないモノが指定された際には、「食べ物以外は受け付けません」という旨の文章を返し、レシピは表示しない。 \n" +
            "・一般的に食べ物とされていない動物や植物（ライオンやカメ、虫など）が指定された際には、「一般的に食用とされていないものは受け付けません」という旨の文章を返し、レシピは表示しない。\n" +
            "・文章はできるだけ日本語を使用する。" +
            "・なるべく日本で一般的な家庭料理を提案する。" +
            "・作り方は料理初心者に向けて提案する気持ちで懇切丁寧に教えてあげる。"+
            "・日本で一般的な食材の切り方をする。" +
            "・実際にそのレシピ通りにつくれば、本当においしい料理ができるような作り方を提案する。"
            ,
            food.getFoodName()
        );

        // ★ .call() ではなく .stream() を使うことで、文字が生成されるたびに順次JavaScriptに送信されます
        return chatModel.stream(prompt);
    }
}
