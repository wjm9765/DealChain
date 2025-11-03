package com.example.sogeding_certification.service;

import com.example.sogeding_certification.dto.UserRequest;
import com.example.sogeding_certification.dto.UserResponse;
import com.example.sogeding_certification.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserResponse verifyUser(UserRequest request) {
        boolean exists = userRepository
                .findByNameAndPhoneNumberAndResidentNumber(
                        request.getName(),
                        request.getPhoneNumber(),
                        request.getResidentNumber()
                )
                .isPresent();

        if (exists) {
            return new UserResponse(true, "인증 성공");
        } else {
            return new UserResponse(false, "인증 실패: 유저 정보를 찾을 수 없습니다");
        }
    }
}

