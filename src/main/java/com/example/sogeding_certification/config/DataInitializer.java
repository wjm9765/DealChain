package com.example.sogeding_certification.config;

import com.example.sogeding_certification.entity.User;
import com.example.sogeding_certification.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer {
    private final UserRepository userRepository;

    @PostConstruct
    public void init() {
        // 테스트용 초기 유저 데이터
        userRepository.save(new User("홍길동", "010-1234-5678", "123456-1234567"));
        userRepository.save(new User("김철수", "010-9876-5432", "987654-9876543"));
        userRepository.save(new User("이영희", "010-1111-2222", "111111-2222222"));
        userRepository.save(new User("박민수", "010-2345-6789", "234567-2345678"));
        userRepository.save(new User("정수진", "010-3456-7890", "345678-3456789"));
        userRepository.save(new User("최동현", "010-4567-8901", "456789-4567890"));
        userRepository.save(new User("한소영", "010-5678-9012", "567890-5678901"));
        userRepository.save(new User("윤성호", "010-6789-0123", "678901-6789012"));
        userRepository.save(new User("임지은", "010-7890-1234", "789012-7890123"));
        userRepository.save(new User("강태현", "010-8901-2345", "890123-8901234"));
    }
}

