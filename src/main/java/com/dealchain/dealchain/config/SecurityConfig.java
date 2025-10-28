package com.dealchain.dealchain.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // 모든 요청 허용
                .requestMatchers("/h2-console/**").permitAll()
                // 회원 관련 API는 인증 없이 접근 가능
                .requestMatchers("/api/members/**").permitAll()
                // 상품 등록, 삭제는 인증 필요
                .requestMatchers("/api/products/create", "/api/products/*/delete").authenticated()
                // 상품 조회는 인증 없이 접근 가능
                .requestMatchers("/api/products/**").permitAll()
                .anyRequest().permitAll()
            )
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}