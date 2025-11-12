package com.dealchain.dealchain.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                //로컬 환경 테스트를 위함
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();

                    //나중에 삭제 필요
                    config.setAllowedOriginPatterns(List.of("http://localhost:*"));


                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setAllowCredentials(true);
                    return config;
                }))

                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )               
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"success\":false,\"message\":\"인증이 필요합니다.\"}");
                        })
                )
                .authorizeHttpRequests(authz -> authz
                        // permitAll 경로 먼저 명시 (더 구체적인 경로부터)
                        .requestMatchers("/api/members/register").permitAll()
                        .requestMatchers("/api/members/login").permitAll()
                        .requestMatchers("/api/products/list").permitAll()
                        .requestMatchers("/api/products/{id}/image").permitAll()  // 상품 이미지는 공개 접근 허용
                        .requestMatchers("/ws/**", "/ws").permitAll()
                        .requestMatchers("/static/**", "/uploads/**").permitAll()

                        // authenticated 경로 명시 (더 구체적인 경로부터)
                        .requestMatchers("/api/members/**").authenticated()
                        .requestMatchers("/api/products/create").authenticated()
                        .requestMatchers("/api/products/{id}/**").authenticated()
                        .requestMatchers("/api/products/{id}").authenticated()
                        .requestMatchers("/api/products/member/**").authenticated()
                        .requestMatchers("/api/contracts/sign").authenticated()
                        .requestMatchers("/api/contracts/create").authenticated()
                        .requestMatchers("/api/contracts/upload").authenticated()
                        .requestMatchers("/api/contracts/{id}").authenticated()
                        .requestMatchers("/api/chat/**").authenticated()
                        
                        // 마지막에 anyRequest
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}