package com.dealchain.dealchain.domain.member;

import com.dealchain.dealchain.domain.api.VerifyService;
import com.dealchain.dealchain.domain.api.dto.VerifyResponseDto;
import com.dealchain.dealchain.domain.security.S3UploadService;
import com.dealchain.dealchain.util.EncryptionUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
@Transactional(transactionManager = "memberTransactionManager")
public class MemberService {
    private final MemberRepository memberRepository;
    private final EncryptionUtil encryptionUtil;
    private final S3UploadService s3UploadService;
    private final VerifyService verifyService;

    public MemberService(MemberRepository memberRepository, EncryptionUtil encryptionUtil, S3UploadService s3UploadService, VerifyService verifyService) {
        this.memberRepository = memberRepository;
        this.encryptionUtil = encryptionUtil;
        this.s3UploadService = s3UploadService;
        this.verifyService = verifyService;
    }

    // 회원가입 (서명 이미지 포함)
    public Member register(String name, String residentNumber, String phoneNumber, String signatureImage) {
        try {
            // 본인 인증 수행
            VerifyResponseDto verifyResponse;
            try {
                verifyResponse = verifyService.verify(name, phoneNumber, residentNumber);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("인증 실패: 유저 정보를 찾을 수 없습니다", e);
            }
            
            if (verifyResponse == null || !verifyResponse.getSuccess()) {
                String errorMessage = verifyResponse != null && verifyResponse.getMessage() != null 
                    ? verifyResponse.getMessage() 
                    : "인증 실패: 유저 정보를 찾을 수 없습니다";
                throw new IllegalArgumentException(errorMessage);
            }

            // 이름/주민번호/전화번호 암호화
            String encryptedName = encryptionUtil.encryptString(name);
            String encryptedResidentNumber = encryptionUtil.encryptString(residentNumber);
            String encryptedPhoneNumber = encryptionUtil.encryptString(phoneNumber);

            // 암호화된 주민번호로 중복 체크
            if (memberRepository.existsByResidentNumber(encryptedResidentNumber)) {
                throw new IllegalArgumentException("이미 가입된 주민번호입니다.");
            }

            Member member = new Member(encryptedName, encryptedResidentNumber, encryptedPhoneNumber, signatureImage);
            return memberRepository.save(member);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("회원가입 중 오류가 발생했습니다.", e);
        }
    }

    //회원가입 - 서명이미지를 s3에 저장
    public Member register(String name, String residentNumber, String phoneNumber, MultipartFile signatureFile) {
        try {
            // 본인 인증 수행
            VerifyResponseDto verifyResponse;
            try {
                verifyResponse = verifyService.verify(name, phoneNumber, residentNumber);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("인증 실패: 유저 정보를 찾을 수 없습니다", e);
            }
            
            if (verifyResponse == null || !verifyResponse.getSuccess()) {
                String errorMessage = verifyResponse != null && verifyResponse.getMessage() != null 
                    ? verifyResponse.getMessage() 
                    : "인증 실패: 유저 정보를 찾을 수 없습니다";
                throw new IllegalArgumentException(errorMessage);
            }

            String encryptedName = encryptionUtil.encryptString(name);
            String encryptedResidentNumber = encryptionUtil.encryptString(residentNumber);
            String encryptedPhoneNumber = encryptionUtil.encryptString(phoneNumber);

            // 주민번호 암호화 및 중복 체크
            if (memberRepository.existsByResidentNumber(encryptedResidentNumber)) {
                throw new IllegalArgumentException("이미 가입된 주민번호입니다.");
            }

            // signatureFile이 제공되면 유효성 검사 및 S3 업로드
            String signatureUrl = null;
            if (signatureFile != null && !signatureFile.isEmpty()) {
                signatureUrl = s3UploadService.upload(signatureFile, "signatures");
            }

            Member member = new Member(encryptedName, encryptedResidentNumber, encryptedPhoneNumber, signatureUrl);
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
            // 입력값 암호화하여 DB의 암호화된 값과 직접 비교
            String encryptedName = encryptionUtil.encryptString(name);
            String encryptedResidentNumber = encryptionUtil.encryptString(residentNumber);
            String encryptedPhoneNumber = encryptionUtil.encryptString(phoneNumber);

            Optional<Member> memberOpt = memberRepository
                    .findByNameAndResidentNumberAndPhoneNumber(encryptedName, encryptedResidentNumber, encryptedPhoneNumber);

            Member member = memberOpt.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

            // 반환용으로 복호화된 사본 생성
            Member decrypted = new Member(
                    encryptionUtil.decryptString(member.getName()),
                    encryptionUtil.decryptString(member.getResidentNumber()),
                    encryptionUtil.decryptString(member.getPhoneNumber()),
                    member.getSignatureImage()
            );
            decrypted.setId(member.getId());
            return decrypted;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("로그인 중 오류가 발생했습니다.", e);
        }
    }

    // 회원 정보 조회 (이름/주민번호/전화번호 복호화하여 반환)
    @Transactional(readOnly = true, transactionManager = "memberTransactionManager")
    public Member findById(Long id) {
        try {
            Member member = memberRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

            // 이름/주민번호/전화번호 복호화하여 새로운 Member 객체 생성 (원본은 수정하지 않음)
            Member decryptedMember = new Member(
                    encryptionUtil.decryptString(member.getName()),
                    encryptionUtil.decryptString(member.getResidentNumber()),
                    encryptionUtil.decryptString(member.getPhoneNumber()),
                    member.getSignatureImage()
            );
            decryptedMember.setId(member.getId());

            return decryptedMember;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("회원 정보 조회 중 오류가 발생했습니다.", e);
        }
    }
}
