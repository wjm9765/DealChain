package com.dealchain.dealchain.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    
    // 주민번호로 회원 찾기 (로그인용)
    Optional<Member> findByResidentNumber(String residentNumber);
    
    // 주민번호 중복 체크용
    boolean existsByResidentNumber(String residentNumber);
}
