package com.dealchain.dealchain.domain.AI.service; // DTO 패키지 대신 service 패키지를 가정합니다.

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlaskApiService {

    // [확인 완료] WebClientConfig.java의 @Bean public WebClient flaskApiWebClient()
    //            메서드 이름과 일치하는 빈을 Spring이 자동으로 주입합니다. (이름: flaskApiWebClient)
    private final WebClient flaskApiWebClient;

    public String sendPostRequest(String jsonBody) {
        log.info("Flask API POST 요청 시작. 본문 길이: {}", jsonBody.length());

        try {
            // WebClient를 사용하여 POST 요청 실행
            String responset = flaskApiWebClient.post()
                    .uri("/") // ngrok 주소의 루트 경로('/')로 요청
                    .bodyValue(jsonBody) // String(JSON)을 본문으로 설정
                    .retrieve()
                    // 2xx 상태 코드가 아닌 경우 예외 발생
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .map(body -> new RuntimeException("Flask API 오류 (" + response.statusCode() + "): " + body)))
                    .bodyToMono(String.class) // 응답 본문을 String으로 변환
                    .block(); // 동기식으로 대기하여 결과(String)를 받음

            log.info("Flask API 응답 수신 성공.");
            return responset;

        } catch (WebClientResponseException e) {
            log.error("Flask API 호출 실패 (상태 코드: {}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            return "{\"error\": \"API 호출 실패\", \"status\": \"" + e.getStatusCode() + "\", \"details\": \" " + e.getResponseBodyAsString() + "\"}";
        } catch (Exception e) {
            log.error("Flask API 호출 중 일반 오류 발생: {}", e.getMessage());
            return "{\"error\": \"일반 오류\", \"details\": \"" + e.getMessage() + "\"}";
        }
    }
}