package com.example.sogeding_certification.controller;

import com.example.sogeding_certification.dto.UserRequest;
import com.example.sogeding_certification.dto.UserResponse;
import com.example.sogeding_certification.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/verify")
    public ResponseEntity<UserResponse> verifyUser(@RequestBody UserRequest request) {
        UserResponse response = userService.verifyUser(request);
        return ResponseEntity.ok(response);
    }
}



