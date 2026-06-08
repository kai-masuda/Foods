package com.example.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests((authorize) -> authorize
                    .requestMatchers("/css/**", "/js/**", "/images/**", "/register").permitAll()
                    .anyRequest().authenticated()
            )
            .formLogin((form) -> form
                    .loginProcessingUrl("/login")
                    .loginPage("/login")
                    .defaultSuccessUrl("/foods")
                    .failureUrl("/login?error")
                    .permitAll()
            )
            .logout(logout -> logout
                    .logoutSuccessUrl("/login?logout")
                    .permitAll()
            );
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}