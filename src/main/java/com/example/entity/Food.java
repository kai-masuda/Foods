package com.example.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
    
    @Column(length =  255, nullable = false)
    @NotBlank(message = "食材名は必須です")
    @Size(max = 255)
    private String foodName;
    
    @Column(length = 255, nullable = false)
    @NotBlank(message = "量は必須です")
    @Size(max = 255)
    private Integer amount;
    
    @Column(nullable = false)
    private LocalDate taste_limit;
    
    @Column(nullable = false)
    private Category category;
    
    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime created_at;
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updated_at;
    

}
