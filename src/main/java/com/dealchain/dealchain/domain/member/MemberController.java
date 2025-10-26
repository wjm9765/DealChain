package com.dealchain.dealchain.domain.member;

import com.dealchain.dealchain.config.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    
    // 로그인 API
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String residentNumber = request.get("residentNumber");
            String phoneNumber = request.get("phoneNumber");
            
            Member member = memberService.login(name, residentNumber, phoneNumber);
            
            // JWT 토큰 생성
            String token = jwtUtil.generateToken(member.getId(), member.getName());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "로그인 성공");
            response.put("token", token);
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
