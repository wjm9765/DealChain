package com.dealchain.dealchain.domain.member;

import com.dealchain.dealchain.domain.security.S3UploadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;

@Service
@Transactional(transactionManager = "memberTransactionManager")
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3UploadService s3UploadService;
    private final RestTemplate restTemplate;
    
    @Value("${verify.api.url}")
    private String verifyApiUrl;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder, S3UploadService s3UploadService) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.s3UploadService = s3UploadService;
        this.restTemplate = new RestTemplate();
    }

    // 회원가입 - id, password, token, signatureImage 받음
    public Member register(String id, String password, String token, MultipartFile signatureFile) {
        try {
            // id 중복 체크
            if (memberRepository.existsByIdString(id)) {
                throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
            }

            // token으로 verify.api.url/{token}에 GET 요청 보내서 name, ci 받아오기
            String tokenUrl = verifyApiUrl + "/" + token;
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !Boolean.TRUE.equals(responseBody.get("success"))) {
                throw new IllegalArgumentException("유효하지 않거나 만료된 토큰입니다.");
            }
            
            String name = (String) responseBody.get("name");
            String ci = (String) responseBody.get("ci");
            
            if (name == null || ci == null) {
                throw new IllegalArgumentException("토큰에서 이름 또는 CI 정보를 가져올 수 없습니다.");
            }

            // 비밀번호 암호화
            String encodedPassword = passwordEncoder.encode(password);

            // signatureFile이 제공되면 유효성 검사 및 S3 업로드
            String signatureUrl = null;
            if (signatureFile != null && !signatureFile.isEmpty()) {
                signatureUrl = s3UploadService.upload(signatureFile, "signatures");
            }

            // id, password, name, ci, signatureImage 저장
            Member member = new Member(id, encodedPassword, name, ci, signatureUrl);
            return memberRepository.save(member);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
            throw new RuntimeException("회원가입 중 오류가 발생했습니다.", e);
        }
    }


    // 로그인 (id, password로 회원 찾기)
    @Transactional(readOnly = true, transactionManager = "memberTransactionManager")
    public Member login(String id, String password) {
        try {
            Optional<Member> memberOpt = memberRepository.findByIdString(id);
            
            Member member = memberOpt.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

            // 비밀번호 검증
            if (!passwordEncoder.matches(password, member.getPassword())) {
                throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
            }

            return member;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("로그인 중 오류가 발생했습니다.", e);
        }
    }

    // 회원 정보 조회
    @Transactional(readOnly = true, transactionManager = "memberTransactionManager")
    public Member findById(Long memberId) {
        try {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

            return member;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("회원 정보 조회 중 오류가 발생했습니다.", e);
        }
    }
}
