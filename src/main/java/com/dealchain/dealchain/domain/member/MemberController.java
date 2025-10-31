// java
package com.dealchain.dealchain.domain.member;

import com.dealchain.dealchain.config.JwtUtil;
import com.dealchain.dealchain.domain.member.dto.MemberLoginRequestDto;
import com.dealchain.dealchain.domain.security.S3UploadService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @Autowired
    private S3UploadService s3UploadService;

    // 회원가입 API (이미지 포함) - 이미지 파일을 로컬에 저장하지 않고 서비스로 전달
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @RequestParam("name") String name,
            @RequestParam("residentNumber") String residentNumber,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam(value = "signatureImage", required = false) MultipartFile signatureImage) {
        try {
            if (signatureImage == null || signatureImage.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "서명 이미지는 필수입니다.");
                return ResponseEntity.badRequest().body(response);
            }

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
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody MemberLoginRequestDto requestDto,
                                                      HttpServletResponse response) {
        try {
            Member member = memberService.login(requestDto.getName(), requestDto.getResidentNumber(), requestDto.getPhoneNumber());
            
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

    // 서명 이미지 조회 API (S3에서 이미지를 가져와서 반환)
    @GetMapping("/signature/{memberId}")
    public ResponseEntity<byte[]> getSignatureImage(@PathVariable("memberId") Long memberId) {
        try {
            Member member = memberService.findById(memberId);
            if (member == null || member.getSignatureImage() == null || member.getSignatureImage().isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // S3 키 (경로) 가져오기
            String fileKey = member.getSignatureImage();

            // S3에서 파일 다운로드 (Content-Type 포함)
            S3UploadService.FileDownloadResult downloadResult = s3UploadService.downloadFileWithContentType(fileKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(downloadResult.getContentType()));
            headers.setContentLength(downloadResult.getFileBytes().length);

            return new ResponseEntity<>(downloadResult.getFileBytes(), headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
