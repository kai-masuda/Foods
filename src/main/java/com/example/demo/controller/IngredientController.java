package com.example.demo.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.Ingredient;
import com.example.demo.repository.IngredientRepository;

@Controller
@RequestMapping("/ingredients")
public class IngredientController {

    @Autowired
    private IngredientRepository repository;

    // 一覧表示・検索・今日の日付表示
    @GetMapping
    public String list(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        List<Ingredient> list;
        if (keyword != null && !keyword.isEmpty()) {
            list = repository.findByNameContainingOrderByExpiryDateAsc(keyword); // 検索
        } else {
            list = repository.findAllByOrderByExpiryDateAsc(); // 期限順ソート一覧
        }
        model.addAttribute("ingredients", list);
        model.addAttribute("today", LocalDate.now()); // 今日の日付
        return "index";
    }

    // 食材登録
    @PostMapping("/add")
    public String add(@ModelAttribute Ingredient ingredient) {
        repository.save(ingredient);
        return "redirect:/ingredients";
    }

    // 任意の量で消費するボタンの処理
    @PostMapping("/consume/{id}")
    public String consume(@PathVariable Long id, @RequestParam("amount") Double amount) {
        Ingredient item = repository.findById(id).orElseThrow();
        item.setQuantity(item.getQuantity() - amount);
        
        if (item.getQuantity() <= 0) {
            repository.delete(item); // 0以下になったら自動削除
        } else {
            repository.save(item); // 残量があれば更新
        }
        return "redirect:/ingredients";
    }
}
