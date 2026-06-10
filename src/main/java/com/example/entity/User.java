package com.example.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;

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
 // ❌ もし @Column(length = 20) がついていたら削除、または長さを広げてください
    @Column(nullable = false, length = 60) // BCryptハッシュに対応するため60以上が必要
    private String password;


    @Column(nullable = false)
    private String role = "USER";
}
