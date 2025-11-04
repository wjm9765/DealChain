package com.example.sogeding_certification.controller;

import com.example.sogeding_certification.dto.UserRequest;
import com.example.sogeding_certification.dto.UserResponse;
import com.example.sogeding_certification.service.TokenService;
import com.example.sogeding_certification.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserApiController {
    private final UserService userService;
    private final TokenService tokenService;

    @PostMapping("/verify")
    public ResponseEntity<UserResponse> verifyUser(@RequestBody UserRequest request) {
        UserResponse response = userService.verifyUser(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/token/{token}")
    public ResponseEntity<Map<String, Object>> getTokenData(@PathVariable String token) {
        TokenService.TokenData data = tokenService.getTokenData(token);
        if (data != null) {
            Map<String, Object> response = Map.of(
                    "success", true,
                    "name", data.getName(),
                    "ci", data.getCi()
            );
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "유효하지 않거나 만료된 토큰입니다"
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
}

