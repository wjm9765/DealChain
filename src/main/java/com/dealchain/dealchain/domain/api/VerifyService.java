package com.dealchain.dealchain.domain.api;

import com.dealchain.dealchain.domain.api.dto.VerifyRequestDto;
import com.dealchain.dealchain.domain.api.dto.VerifyResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class VerifyService {

    @Value("${verify.api.url}")
    private String verifyApiUrl;
    private final RestTemplate restTemplate;

    public VerifyService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 외부 API로 본인 인증 요청을 보냅니다.
     * 
     * @param name 이름
     * @param phoneNumber 전화번호
     * @param residentNumber 주민번호
     * @return VerifyResponseDto 응답 객체
     */
    public VerifyResponseDto verify(String name, String phoneNumber, String residentNumber) {
        VerifyRequestDto requestDto = new VerifyRequestDto(name, phoneNumber, residentNumber);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<VerifyRequestDto> request = new HttpEntity<>(requestDto, headers);
        
        try {
            ResponseEntity<VerifyResponseDto> response = restTemplate.exchange(
                verifyApiUrl,
                HttpMethod.POST,
                request,
                VerifyResponseDto.class
            );
            return response.getBody();
        } catch (Exception e) {
            // 예외 처리 - 필요에 따라 커스텀 예외로 변환 가능
            throw new RuntimeException("본인 인증 API 호출 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * VerifyRequestDto를 사용하여 본인 인증 요청을 보냅니다.
     * 
     * @param requestDto 요청 DTO
     * @return VerifyResponseDto 응답 객체
     */
    public VerifyResponseDto verify(VerifyRequestDto requestDto) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<VerifyRequestDto> request = new HttpEntity<>(requestDto, headers);
        
        try {
            ResponseEntity<VerifyResponseDto> response = restTemplate.exchange(
                verifyApiUrl,
                HttpMethod.POST,
                request,
                VerifyResponseDto.class
            );
            return response.getBody();
        } catch (Exception e) {
            // 예외 처리 - 필요에 따라 커스텀 예외로 변환 가능
            throw new RuntimeException("본인 인증 API 호출 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}

