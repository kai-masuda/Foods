package com.example.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "ユーザー名は必須です")
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank(message = "パスワードは必須です")
    @Column(nullable = false)
    @Size(min = 6, max=20, message = "パスワードは{min}文字以上{max}文字以下で入力して下さい")
    private String password;

    @Column(nullable = false)
    private String role = "USER";
}
