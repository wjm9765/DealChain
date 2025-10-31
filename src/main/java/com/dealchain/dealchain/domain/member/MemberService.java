package com.dealchain.dealchain.domain.member;

import com.dealchain.dealchain.domain.security.S3UploadService;
import com.dealchain.dealchain.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Transactional(transactionManager = "memberTransactionManager")
public class MemberService {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EncryptionUtil encryptionUtil;

    @Autowired
    private S3UploadService s3UploadService;

    // 회원가입
    public Member register(String name, String residentNumber, String phoneNumber) {
        try {
            // 주민번호 암호화
            String encryptedResidentNumber = encryptionUtil.encryptString(residentNumber);

            // 암호화된 주민번호로 중복 체크
            if (memberRepository.existsByResidentNumber(encryptedResidentNumber)) {
                throw new IllegalArgumentException("이미 가입된 주민번호입니다.");
            }

            Member member = new Member(name, encryptedResidentNumber, phoneNumber);
            return memberRepository.save(member);
        } catch (Exception e) {
            throw new RuntimeException("회원가입 중 오류가 발생했습니다.", e);
        }
    }

    // 회원가입 (서명 이미지 포함)
    public Member register(String name, String residentNumber, String phoneNumber, String signatureImage) {
        try {
            // 주민번호 암호화
            String encryptedResidentNumber = encryptionUtil.encryptString(residentNumber);

            // 암호화된 주민번호로 중복 체크
            if (memberRepository.existsByResidentNumber(encryptedResidentNumber)) {
                throw new IllegalArgumentException("이미 가입된 주민번호입니다.");
            }

            Member member = new Member(name, encryptedResidentNumber, phoneNumber, signatureImage);
            return memberRepository.save(member);
        } catch (Exception e) {
            throw new RuntimeException("회원가입 중 오류가 발생했습니다.", e);
        }
    }

    //회원가입 - 서명이미지를 s3에 저장
    public Member register(String name, String residentNumber, String phoneNumber, MultipartFile signatureFile) {
        try {

            // 주민번호 암호화 및 중복 체크
            String encryptedResidentNumber = encryptionUtil.encryptString(residentNumber);
            if (memberRepository.existsByResidentNumber(encryptedResidentNumber)) {
                throw new IllegalArgumentException("이미 가입된 주민번호입니다.");
            }

            // signatureFile이 제공되면 유효성 검사 및 S3 업로드
            String signatureUrl = null;
            if (signatureFile != null && !signatureFile.isEmpty()) {
                signatureUrl = s3UploadService.upload(signatureFile, "signatures");
            }

            Member member = new Member(name, encryptedResidentNumber, phoneNumber, signatureUrl);
            return memberRepository.save(member);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("회원가입 중 오류가 발생했습니다.", e);
        }
    }


    // 로그인 (이름, 주민번호, 전화번호로 회원 찾기)
    @Transactional(readOnly = true, transactionManager = "memberTransactionManager")
    public Member login(String name, String residentNumber, String phoneNumber) {
        try {
            // 모든 멤버를 가져와서 복호화된 주민번호와 비교
            List<Member> allMembers = memberRepository.findAll();

            for (Member member : allMembers) {
                try {
                    String decryptedResidentNumber = encryptionUtil.decryptString(member.getResidentNumber());
                    if (name.equals(member.getName()) &&
                            residentNumber.equals(decryptedResidentNumber) &&
                            phoneNumber.equals(member.getPhoneNumber())) {
                        return member;
                    }
                } catch (Exception e) {
                    // 복호화 실패 시 해당 멤버는 건너뛰기
                    continue;
                }
            }

            throw new IllegalArgumentException("존재하지 않는 회원입니다.");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("로그인 중 오류가 발생했습니다.", e);
        }
    }

    // 회원 정보 조회 (복호화된 주민번호 반환)
    @Transactional(readOnly = true, transactionManager = "memberTransactionManager")
    public Member findById(Long id) {
        try {
            Member member = memberRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

            // 주민번호 복호화
            String decryptedResidentNumber = encryptionUtil.decryptString(member.getResidentNumber());

            // 복호화된 주민번호로 새로운 Member 객체 생성 (원본은 수정하지 않음)
            Member decryptedMember = new Member(member.getName(), decryptedResidentNumber, member.getPhoneNumber(), member.getSignatureImage());
            decryptedMember.setId(member.getId());

            return decryptedMember;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("회원 정보 조회 중 오류가 발생했습니다.", e);
        }
    }
}
