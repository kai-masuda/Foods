package com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.entity.User;
import com.example.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional 
    public User registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("ROLE_USER");
        return userRepository.save(user);
    }
    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    // ★FoodControllerのためにこのメソッドを追加します
    public User findByUsername(String username) {
        // リポジトリが返す Optional<User> から .orElse(null) で Userオブジェクト を取り出します
        return userRepository.findByUsername(username).orElse(null);
    }
}
