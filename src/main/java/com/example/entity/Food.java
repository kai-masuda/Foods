package com.example.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;  // 【追加】
import jakarta.persistence.PreUpdate;  // 【追加】
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.format.annotation.DateTimeFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Food {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 255, nullable = false)
    @NotBlank(message = "食材名は必須です")
    @Size(max = 20)
    private String foodName;
    
    @Column(nullable = false)
    //【修正】MinからCecimalMinに変更し、最小値を1から0.1に変更
    @jakarta.validation.constraints.DecimalMin(value = "0.1", message = "量は0.1以上で入力してください") // 数値用の必須・最小値チェック
    //【修正】int→double型に変更
    private double amount;

    //【修正】taste_limit→tasteLimitに変更
    @Column(nullable = false)
    //【修正】日付のフォーマット入れることで編集画面で日付がリセットされないように変更
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate tasteLimit;
    

    // @ManyToOneで複数の食材が1つのカテゴリに属する(多対1)
    @ManyToOne
    // @JoinColumnで外部キーであるcategory_idをカラム名として指定
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    

 // @ManyToOneで複数の食材が1つのユーザーに属する(多対1)
 @ManyToOne
 // @JoinColumnで外部キーであるuser_idをカラム名として指定
 @JoinColumn(name = "user_id", nullable = false)
 private User user; // ★これを追加

    
    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime created_at;
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updated_at;
    
    @PrePersist
    public void onPrePersist() {
        setCreated_at(LocalDateTime.now());
        setUpdated_at(LocalDateTime.now());
    }
    
    @PreUpdate
    public void onPreUpdate() {
        setUpdated_at(LocalDateTime.now());
    }
    
    //【追加】期限が切れているか判定するメソッド（今日より前）
    public boolean isExpired() {
        if (this.tasteLimit == null) return false;
        return this.tasteLimit.isBefore(LocalDate.now());
    }

    //【追加】期限が近いか判定するメソッド（今日を含めて3日以内）
    public boolean isUrgent() {
        if (this.tasteLimit == null) return false;
        LocalDate today = LocalDate.now();
        // 今日以降、かつ、今日から3日後以内であればtrue
        return !this.tasteLimit.isBefore(today) && !this.tasteLimit.isAfter(today.plusDays(3));
    }

}


