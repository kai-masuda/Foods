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

 // 一覧表示・検索
    @GetMapping
    public String list(@RequestParam(name = "keyword", required = false, defaultValue = "") String keyword, Model model) {
        List<Ingredient> list;
        
        // キーワードが空っぽ（空白文字含む）でなければ検索、そうでないなら全件取得
        if (keyword != null && !keyword.trim().isEmpty()) {
            list = repository.findByNameContainingOrderByExpiryDateAsc(keyword);
        } else {
            list = repository.findAllByOrderByExpiryDateAsc();
        }
        
        model.addAttribute("ingredients", list);
        model.addAttribute("keyword", keyword); // 画面に検索ワードを残すために追加
        model.addAttribute("today", LocalDate.now());
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
    public String consume(@PathVariable Long id, @RequestParam("amount") Integer amount) {
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
