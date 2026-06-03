package com.example.entity;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length =  255, nullable = false)
    @NotBlank(message = "カテゴリ名は必須です")
    @Size(max = 255)
    private String categoryName;
    
    @Column(length = 255, nullable = false)
    @NotBlank(message = "単位は必須です")
    @Size(max = 255)
    private String unit;
    
<<<<<<< HEAD
    // @OneToManyで1つのカテゴリに対して複数の食材が紐づく(1対多)
=======
>>>>>>> branch 'master' of https://github.com/kai-masuda/Foods.git
    @OneToMany(mappedBy = "category")
    private List<Food> foods;

}
