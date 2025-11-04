package com.example.sogeding_certification.service;

import com.example.sogeding_certification.dto.UserRequest;
import com.example.sogeding_certification.dto.UserResponse;
import com.example.sogeding_certification.entity.User;
import com.example.sogeding_certification.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final TokenService tokenService;
    
    @Value("${app.redirect.url}")
    private String redirectUrl;

    public UserResponse verifyUser(UserRequest request) {
        Optional<User> userOpt = userRepository
                .findByNameAndPhoneNumberAndResidentNumber(
                        request.getName(),
                        request.getPhoneNumber(),
                        request.getResidentNumber()
                );

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // 토큰 생성 및 저장
            String token = tokenService.generateToken(user.getName(), user.getCi());
            // redirect URL에 토큰 전달
            String redirectUrlWithToken = redirectUrl + "?token=" + token;
            
            return new UserResponse(true, "인증 성공", redirectUrlWithToken, user.getName(), user.getCi());
        } else {
            return new UserResponse(false, "인증 실패: 유저 정보를 찾을 수 없습니다", null, null, null);
        }
    }
}

