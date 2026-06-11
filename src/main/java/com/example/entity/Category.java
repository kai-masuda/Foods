package com.example.entity;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode; // 【追加】
import lombok.NoArgsConstructor;
import lombok.ToString;         // 【追加】

@Entity
@Data//ゲッターセッターを自動作成するアノテーション
@NoArgsConstructor//コンストラクター自動生成
@AllArgsConstructor
public class Category {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length =  255, nullable = false)
    @NotBlank(message = "カテゴリ名は必須です")
    @Size(max = 20, message = "カテゴリ名は20文字以内で入力してください")
    //【追加】日本語（ひらがな・カタカナ・漢字）と英字のみ許可（数字や記号はエラー）
    @Pattern(
            regexp = "^[a-zA-Zぁ-んァ-ヶー一-龠々]+$",
            message = "カテゴリ名には文字のみ入力してください(数字や記号は使用できません)"
            )
    private String categoryName;
    
 // @ManyToOneで複数の食材が1つのカテゴリに属する(多対1)
    @ManyToOne(fetch = FetchType.EAGER)
    // @JoinColumnで外部キーであるcategory_idをカラム名として指定
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;
    

 // 【修正】循環参照（無限ループ）を防止する設定を追加
    @OneToMany(mappedBy = "category")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Food> foods;

}

