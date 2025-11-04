package com.example.sogeding_certification.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {
    private final Map<String, TokenData> tokenStore = new ConcurrentHashMap<>();

    public String generateToken(String name, Long ci) {
        String token = UUID.randomUUID().toString();
        tokenStore.put(token, new TokenData(name, ci));
        return token;
    }

    public TokenData getTokenData(String token) {
        return tokenStore.get(token);
    }

    @Data
    @AllArgsConstructor
    public static class TokenData {
        private String name;
        private Long ci;
    }
}
