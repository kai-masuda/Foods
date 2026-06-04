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
    @Size(max = 255)
    private String foodName;
    
    @Column(nullable = false)
    @jakarta.validation.constraints.Min(value = 1, message = "量は1以上で入力してください") // 数値用の必須・最小値チェック
    private int amount;

    
    @Column(nullable = false)
    private LocalDate taste_limit;
    

    // @ManyToOneで複数の食材が1つのカテゴリに属する(多対1)
    @ManyToOne
    // @JoinColumnで外部キーであるcategory_idをカラム名として指定
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    
    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime created_at;
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updated_at;
    
    // 【追加】動いていたアプリと全く同じコールバック処理
    @PrePersist
    public void onPrePersist() {
        setCreated_at(LocalDateTime.now());
        setUpdated_at(LocalDateTime.now());
    }
    
    @PreUpdate
    public void onPreUpdate() {
        setUpdated_at(LocalDateTime.now());
    }
}


