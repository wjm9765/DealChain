package com.dealchain.dealchain.domain.member;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MemberService {
    
    @Autowired
    private MemberRepository memberRepository;
    
    // 회원가입
    public Member register(String name, String residentNumber, String phoneNumber) {
        // 주민번호 중복 체크
        if (memberRepository.existsByResidentNumber(residentNumber)) {
            throw new IllegalArgumentException("이미 가입된 주민번호입니다.");
        }
        
        Member member = new Member(name, residentNumber, phoneNumber);
        return memberRepository.save(member);
    }
    
    // 로그인 (이름, 주민번호, 전화번호로 회원 찾기)
    @Transactional(readOnly = true)
    public Member login(String name, String residentNumber, String phoneNumber) {
        return memberRepository.findByNameAndResidentNumberAndPhoneNumber(name, residentNumber, phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }
    
    // 회원 정보 조회
    @Transactional(readOnly = true)
    public Member findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }
}
