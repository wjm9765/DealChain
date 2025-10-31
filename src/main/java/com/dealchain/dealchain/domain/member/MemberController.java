// java
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EncryptionUtil encryptionUtil;

    // 회원가입 API (이미지 포함) - 이미지 파일을 로컬에 저장하지 않고 서비스로 전달
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @RequestParam("name") String name,
            @RequestParam("residentNumber") String residentNumber,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam(value = "signatureImage", required = false) MultipartFile signatureImage) {
        try {
            // 서명 이미지를 로컬에 저장하지 않고 그대로 서비스 계층으로 전달
            Member member = memberService.register(name, residentNumber, phoneNumber, signatureImage);

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
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "회원가입 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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
            cookie.setHttpOnly(true);
            cookie.setSecure(false);
            cookie.setPath("/");
            cookie.setMaxAge(86400);

            response.addCookie(cookie);

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", true);
            responseBody.put("message", "로그인 성공");
            responseBody.put("memberId", member.getId());
            responseBody.put("name", member.getName());

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
        Cookie cookie = new Cookie("token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
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

    // 서명 이미지 조회 API (현재 구현은 로컬 암호화 파일을 복호화하여 반환함 - S3 사용 시 변경 필요)
    @GetMapping("/signature/{memberId}")
    public ResponseEntity<byte[]> getSignatureImage(@PathVariable("memberId") Long memberId) {
        try {
            Member member = memberService.findById(memberId);
            if (member == null || member.getSignatureImage() == null) {
                return ResponseEntity.notFound().build();
            }

            String originalPath = member.getSignatureImage();
            String encryptedPath = originalPath.replace("uploads/signatures/", "uploads/signatures_encrypted/");

            Path encryptedFilePath = Paths.get(encryptedPath);
            if (!Files.exists(encryptedFilePath)) {
                return ResponseEntity.notFound().build();
            }

            byte[] encryptedBytes = Files.readAllBytes(encryptedFilePath);
            byte[] decryptedBytes = encryptionUtil.decryptFileBytes(encryptedBytes);

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
}
