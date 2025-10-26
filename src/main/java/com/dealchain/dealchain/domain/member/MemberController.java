package com.dealchain.dealchain.domain.member;

import com.dealchain.dealchain.config.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/members")
public class MemberController {
    
    @Autowired
    private MemberService memberService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    // 회원가입 API
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String residentNumber = request.get("residentNumber");
            String phoneNumber = request.get("phoneNumber");
            
            Member member = memberService.register(name, residentNumber, phoneNumber);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "회원가입이 완료되었습니다.");
            response.put("memberId", member.getId());
            response.put("name", member.getName());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // 로그인 API (HttpOnly Cookie 사용)
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request,
                                                      HttpServletResponse response) {
        try {
            String name = request.get("name");
            String residentNumber = request.get("residentNumber");
            String phoneNumber = request.get("phoneNumber");
            
            Member member = memberService.login(name, residentNumber, phoneNumber);
            
            // JWT 토큰 생성
            String token = jwtUtil.generateToken(member.getId(), member.getName());
            
            // HttpOnly Cookie 설정
            Cookie cookie = new Cookie("token", token);
            cookie.setHttpOnly(true);        // XSS 공격 방지
            cookie.setSecure(false);          // 개발환경에서는 false, 운영환경에서는 true
            cookie.setPath("/");              // 모든 경로에서 사용 가능
            cookie.setMaxAge(86400);         // 24시간 (초 단위)
            // setSameSite는 Spring Boot 2.6+ 에서 지원
            
            response.addCookie(cookie);
            
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", true);
            responseBody.put("message", "로그인 성공");
            responseBody.put("memberId", member.getId());
            responseBody.put("name", member.getName());
            // token은 응답에 포함하지 않음 (보안상 쿠키로만 전송)
            
            return ResponseEntity.ok(responseBody);
        } catch (IllegalArgumentException e) {
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", false);
            responseBody.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(responseBody);
        }
    }
    
    // 로그아웃 API (HttpOnly Cookie 삭제)
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletResponse response) {
        // 쿠키 삭제
        Cookie cookie = new Cookie("token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);  // 즉시 만료
        response.addCookie(cookie);
        
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", "로그아웃되었습니다.");
        
        return ResponseEntity.ok(responseBody);
    }
    
    // 회원 정보 조회 API
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMember(@PathVariable Long id) {
        try {
            Member member = memberService.findById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("member", Map.of(
                "id", member.getId(),
                "name", member.getName(),
                "residentNumber", member.getResidentNumber(),
                "phoneNumber", member.getPhoneNumber()
            ));
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
