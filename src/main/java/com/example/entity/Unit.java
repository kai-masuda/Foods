package com.example.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data//ゲッターセッターを自動作成するアノテーション
@NoArgsConstructor//コンストラクター自動生成
@AllArgsConstructor
public class Unit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length =  255, nullable = false)
    @NotBlank(message = "単位は必須です")
    @Size(max = 10, message = "単位は10文字以内で入力してください")
    //【追加】日本語（ひらがな・カタカナ・漢字）と英字のみ許可（数字や記号はエラー）
    @Pattern(
            regexp = "^[a-zA-Zぁ-んァ-ヶー一-龠々]+$",
            message = "単位には文字のみを入力してください(数字や記号は使用できません)"
            )
    private String unitName;

}
