package com.dealchain.dealchain.domain.member;

import com.dealchain.dealchain.config.JwtUtil;
import com.dealchain.dealchain.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/members")
public class MemberController {
    
    @Autowired
    private MemberService memberService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private EncryptionUtil encryptionUtil;
    
    // 회원가입 API (이미지 포함)
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @RequestParam("name") String name,
            @RequestParam("residentNumber") String residentNumber,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam(value = "signatureImage", required = false) MultipartFile signatureImage) {
        try {
            String signatureImagePath = null;
            
            // 서명 이미지가 있는 경우 저장
            if (signatureImage != null && !signatureImage.isEmpty()) {
                signatureImagePath = saveImage(signatureImage, "signatures");
            }
            
            Member member = memberService.register(name, residentNumber, phoneNumber, signatureImagePath);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "회원가입이 완료되었습니다.");
            response.put("memberId", member.getId());
            response.put("name", member.getName());
            response.put("signatureImage", member.getSignatureImage());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IOException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "이미지 저장 중 오류가 발생했습니다.");
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
    public ResponseEntity<Map<String, Object>> getMember(@PathVariable("id") Long id) {
        try {
            Member member = memberService.findById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            Map<String, Object> memberInfo = new HashMap<>();
            memberInfo.put("id", member.getId());
            memberInfo.put("name", member.getName());
            memberInfo.put("residentNumber", member.getResidentNumber());
            memberInfo.put("phoneNumber", member.getPhoneNumber());
            memberInfo.put("signatureImage", member.getSignatureImage() != null ? member.getSignatureImage() : "");
            
            response.put("member", memberInfo);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // 서명 이미지 조회 API (복호화하여 반환)
    @GetMapping("/signature/{memberId}")
    public ResponseEntity<byte[]> getSignatureImage(@PathVariable("memberId") Long memberId) {
        try {
            Member member = memberService.findById(memberId);
            if (member == null || member.getSignatureImage() == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 데이터베이스의 경로를 암호화된 파일 경로로 변환
            String originalPath = member.getSignatureImage();
            String encryptedPath = originalPath.replace("uploads/signatures/", "uploads/signatures_encrypted/");
            
            // 암호화된 파일을 복호화하여 반환
            Path encryptedFilePath = Paths.get(encryptedPath);
            if (!Files.exists(encryptedFilePath)) {
                return ResponseEntity.notFound().build();
            }
            
            byte[] encryptedBytes = Files.readAllBytes(encryptedFilePath);
            byte[] decryptedBytes = encryptionUtil.decryptFileBytes(encryptedBytes);
            
            // Content-Type 설정 (이미지 타입에 따라)
            String contentType = Files.probeContentType(encryptedFilePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(decryptedBytes.length);
            
            return new ResponseEntity<>(decryptedBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // 이미지 저장 헬퍼 메서드 (서명 이미지 암호화 저장)
    private String saveImage(MultipartFile image, String folder) throws IOException {
        try {
            // 업로드 디렉토리 생성
            String uploadDir = "uploads/" + folder;
            String encryptedDir = "uploads/" + folder + "_encrypted";
            Path uploadPath = Paths.get(uploadDir);
            Path encryptedPath = Paths.get(encryptedDir);
            
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            if (!Files.exists(encryptedPath)) {
                Files.createDirectories(encryptedPath);
            }
            
            // 고유한 파일명 생성
            String originalFilename = image.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = UUID.randomUUID().toString() + extension;
            
            // 원본 파일 저장 (임시)
            Path tempFilePath = uploadPath.resolve("temp_" + filename);
            Files.copy(image.getInputStream(), tempFilePath);
            
            // 암호화된 파일 저장
            String encryptedFilePath = encryptedDir + "/" + filename;
            encryptionUtil.encryptFile(tempFilePath.toString(), encryptedFilePath);
            
            // 임시 파일 삭제
            Files.deleteIfExists(tempFilePath);
            
            // 데이터베이스에는 원본 파일 경로 저장 (보안상 실제로는 암호화된 파일이 저장됨)
            return uploadDir + "/" + filename;
            
        } catch (Exception e) {
            throw new IOException("서명 이미지 암호화 저장 중 오류 발생: " + e.getMessage(), e);
        }
    }
}
