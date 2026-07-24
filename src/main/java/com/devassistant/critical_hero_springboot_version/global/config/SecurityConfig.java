package com.devassistant.critical_hero_springboot_version.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/webhook/**")
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/webhook/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
