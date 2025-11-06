package com.dealchain.dealchain.domain.AI.service;


import com.dealchain.dealchain.domain.AI.dto.FraudDetectionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class DetectService {

    private final WebClient apiGatewayWebClient;

    /**
     * /detect 엔드포인트로 POST 요청을 보냅니다.
     *
     * @param jsonRequest API에 보낼 JSON 문자열
     * @return API의 응답을 FraudDetectionResponse 객체로 변환하여 반환
     */
    public FraudDetectionResponse callDetectEndpoint(String jsonRequest) {
        log.info("API Gateway /detect 호출 시작. 요청: {}", jsonRequest);

        try {
            // WebClient를 사용하여 POST 요청 실행
            FraudDetectionResponse response = apiGatewayWebClient.post()
                    .uri("/detect") // 1. baseUrl 뒤에 붙을 경로 (/detect)
                    .bodyValue(jsonRequest) // 2. 입력받은 String(JSON)을 본문으로 설정
                    .retrieve() // 3. 응답 받기
                    .bodyToMono(FraudDetectionResponse.class) // 4. 응답을 DTO로 변환
                    .block(); // 5. 동기식으로 대기하여 결과 객체를 받음

            log.info("API Gateway /detect 응답 수신: {}", response);
            return response;

        } catch (Exception e) {
            log.error("API Gateway(/detect) 호출 중 오류 발생: {}", e.getMessage());
            return null;
        }
    }
}