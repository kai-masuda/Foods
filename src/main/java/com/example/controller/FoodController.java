package com.example.controller;

import java.security.Principal; // ★ログインユーザー情報取得のために追加
import java.util.List;
import java.util.Set;//【追加】
import java.util.stream.Collectors;

import jakarta.validation.Valid; //【追加】

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
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
import com.example.entity.ShoppingMemo;
import com.example.entity.Unit;
import com.example.entity.User; // ★Userエンティティをインポート
import com.example.repository.FoodRepository;
import com.example.repository.ShoppingMemoRepository;
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
    
    @Autowired
    private ShoppingMemoRepository shoppingMemoRepository;


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

        Long userId = currentUser.getId();

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
        
        List<ShoppingMemo> shoppingMemos = shoppingMemoRepository.findByUserIdOrderByCreatedAtDesc(userId);
        model.addAttribute("shoppingMemos", shoppingMemos);

        model.addAttribute("currentKeyword", keyword);
        model.addAttribute("currentCategoryId", categoryId);
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentDirection", direction);
        model.addAttribute("reverseDirection", direction.equals("asc") ? "desc" : "asc");

        //【追加】usernameを得る
        model.addAttribute("loginUser", currentUser.getUsername());

        return "foods/index";
    }
    
    //買い物メモ
    @PostMapping("/shopping-memo/add")
    public String addToShoppingMemo(@RequestParam("memoText") String memoText, Principal principal) {
        String username = principal.getName();
        User currentUser = userService.findByUsername(username);

        if (currentUser != null && memoText != null && !memoText.trim().isEmpty()) {
            ShoppingMemo memo = new ShoppingMemo();
            memo.setMemoText(memoText.trim());
            memo.setUser(currentUser);
            shoppingMemoRepository.save(memo);
        }
        return "redirect:/foods"; // 登録完了後、食材一覧画面に戻る
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

        //【追加】すでに存在する食材名、期限、カテゴリ名、単位のデータは量を合算する
        // カテゴリと単位を確定させる
        handleCategory(food, categoryName, unit);
        
        // ログインユーザの食材から、名前が一致するものを検索
        List<Food> existingFoods = foodService.searchFoodsByUserId(currentUser.getId(), food.getFoodName(), null, org.springframework.data.domain.Sort.by("id"));
        
        Food duplicateFood = null;
        for (Food existing : existingFoods) {
            // 食材名、カテゴリID、賞味期限がすべて一致するかチェック
            if (existing.getFoodName().equals(food.getFoodName()) &&
                existing.getCategory().getId().equals(food.getCategory().getId()) &&
                existing.getTasteLimit().equals(food.getTasteLimit())) {
                
                duplicateFood = existing;
                break;
            }
        }

        if (duplicateFood != null) {
            // 重複があれば、既存のデータに数量を合算
            double newAmount = duplicateFood.getAmount() + food.getAmount();
            newAmount = Math.round(newAmount * 100.0) / 100.0; // 小数点計算のズレ防止
            
            duplicateFood.setAmount(newAmount);
            foodService.saveFood(duplicateFood);
        } else {
            // 重複がなければ、通常通り新規保存
            foodService.saveFood(food);
        }
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
        
        //【追加】すでに存在する食材名、期限、カテゴリ名、単位のデータは量を合算する
        List<Food> sameNameFoods = foodService.searchFoodsByUserId(currentUser.getId(), food.getFoodName(), null, org.springframework.data.domain.Sort.by("id"));
        
        Food duplicateFood = null;
        for (Food otherFood : sameNameFoods) {
            // 自分自身のID（既存のデータ）以外で、名前・カテゴリID・賞味期限がすべて一致する「別データ」を探す
            if (!otherFood.getId().equals(existingFood.getId()) &&
                otherFood.getFoodName().equals(existingFood.getFoodName()) &&
                otherFood.getCategory().getId().equals(existingFood.getCategory().getId()) &&
                otherFood.getTasteLimit().equals(existingFood.getTasteLimit())) {
                
                duplicateFood = otherFood;
                break;
            }
        }

        if (duplicateFood != null) {
            // 別のデータと被った場合
            // 別の既存データに、今回の入力された数量（food.getAmount()）を足し算する
            double newAmount = duplicateFood.getAmount() + food.getAmount();
            newAmount = Math.round(newAmount * 100.0) / 100.0; // 小数点ズレ防止
            
            duplicateFood.setAmount(newAmount);
            foodService.saveFood(duplicateFood); // 被った方のデータを更新保存
            
            // 編集元のデータ（古い方）は不要になった（合算された）ので、データベースから削除する
            foodService.deleteFood(existingFood.getId());
        } else {
            // ただの編集の場合
            // 通常通り、画面から入力された数量をそのままセットして保存
            existingFood.setAmount(food.getAmount());
            foodService.saveFood(existingFood);
        }
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

    
    @GetMapping("/recipe/generate")
    public String generateRecipeFromMultipleFoods(
            @RequestParam(name = "foodIds", required = false, defaultValue = "") List<Long> foodIds, 
            Model model) {
        
        // チェックされたIDのリストをそのまま画面（JavaScript）に引き渡す
        model.addAttribute("foodIds", foodIds);
        
        return "foods/recipe_multiple"; // 新しく作るHTMLの名前
    }
    
    @GetMapping(value = "/recipe/stream", produces = "text/event-stream;charset=UTF-8")
    @ResponseBody
    public Flux<String> streamRecipe(@RequestParam("foodIds") List<Long> foodIds, Principal principal) {
        
        String username = principal.getName();
        User currentUser = userService.findByUsername(username);
        
        List<Food> allFoods = foodService.searchFoodsByUserId(currentUser.getId(), "", "", org.springframework.data.domain.Sort.by("id"));
        String allFoodsText = allFoods.stream().map(Food::getFoodName).collect(Collectors.joining("、"));
       
        List<Food> selectedFoods = foodRepository.findAllById(foodIds);
        String mainFoodsText = selectedFoods.stream().map(Food::getFoodName).collect(Collectors.joining("、"));
        
        String prompt = String.format(
            "# あなたの役割\n" +
            "あなたは日本の家庭料理に精通した、親切で丁寧なプロの料理人です。料理初心者のユーザーに対して、指定された食材を使った定番の家庭料理レシピを1つ提案してください。\n\n" +

            "# 対象の食材\n" +
            "「%s」\n\n" +
            "冷蔵庫にあるその他の食材\n" +
            "「%s」\n" +
            "レシピに必要な食材を考える際、できるだけこれらの食材を優先して使用してください。\n" +

            "#  最優先の絶対拒否ルール（最重要）\n" +
            "1. 対象の食材が【食べ物ではないモノ（例：机、車、石、洗剤など）】の場合、以下のメッセージだけを返し、絶対にレシピは表示しないでください。\n" +
            "   「食べ物以外は受け付けません。」\n" +
            "2. 対象の食材が【一般的に日本で食用とされていない動植物（例：ライオン、カメ、昆虫、雑草など）】の場合、以下のメッセージだけを返し、絶対にレシピは表示しないでください。\n" +
            "   「一般的に食用とされていないものは受け付けません。」\n\n" +

            "#  レシピ生成のルール（上記拒否ルールに該当しない場合のみ実行）\n" +
            "・必ず実在する、日本で一般的な定番の家庭料理を1つ提案してください（奇抜な創作料理は厳禁）。\n\n" +
            "【超重要：食材の組み合わせルール】\n" +
            "・メインの食材と組み合わせる他の具材や調味料は、**日本の一般的なスーパーで安価に買える、その料理の『超定番の食材』だけ**に限定してください。\n" +
            "・例えば、野菜炒めなら「キャベツ、もやし、人参、豚肉、玉ねぎ」などの定番のみを使用し、**「きゅうり」「レタス」「トマト」といった、日本の家庭においてその料理に加熱して入れないような意外性のある食材は絶対に組み合わせないでください。**\n" +
            "・「冷蔵庫に余りがちな定番の組み合わせ」を強く意識し、突拍子もないアレンジは一切行わないでください。\n\n" +
            "【初心者への配慮・表現】\n" +
            "・料理初心者に向けて、専門用語を使わず、懇切丁寧にステップバイステップで手順を説明してください。\n" +
            "・食材の切り方は、日本で広く使われている一般的な表現（例：いちょう切り、みじん切り、乱切りなど）を使用してください。\n" +
            "・実際にこの通りに作れば、本当に美味しく作れる正確な分量と手順にしてください。\n" +
            "・解説文やフレーズを含め、出力はすべて自然な日本語のみを使用してください。\n\n" +
            "・レシピ生成時に、「レシピに必要な食材」が「冷蔵庫にあるその他の食材」の中にあった場合、その食材の横に◎を書いてください" + 

            "# 出力フォーマット\n" +
            "以下の構成を1つの料理それぞれで繰り返してマークダウン形式で出力してください。\n" +
            "--- \n" +
            " 料理名\n" +
            "1. 材料（分量）\n" +
            "2. 作り方の手順\n" +
            "3. 美味しく作るためのコツ",
            mainFoodsText, allFoodsText);
        
        // 末尾に [DONE] フラグを付与して、安全にフロントに終了を伝える
        return chatModel.stream(prompt)
                .concatWith(Flux.just("\n[DONE]"));
    }
}
