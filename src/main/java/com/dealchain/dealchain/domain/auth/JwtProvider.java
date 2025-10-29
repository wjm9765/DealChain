package com.dealchain.dealchain.domain.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;

import org.springframework.beans.factory.annotation.Value; // <- 이 줄이 필요해요!
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {
    private final Key key;

    public JwtProvider(@Value("${JWT_SECRET}") String secret) {

        this.key = Keys.hmacShaKeyFor(secret.getBytes()); // 문자열을 바이트로 변환하여 Key 생성
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getUserId(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
        return claims.getSubject();
    }
}
