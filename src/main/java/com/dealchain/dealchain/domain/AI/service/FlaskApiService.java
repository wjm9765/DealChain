package com.dealchain.dealchain.domain.AI.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlaskApiService {
    private final WebClient flaskApiWebClient;

    public String sendPostRequest(String jsonBody) {
        try {
            String responset = flaskApiWebClient.post()
                    .uri("/detect_fraud")
                    .bodyValue(jsonBody)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .map(body -> new RuntimeException("Flask API 오류 (" + response.statusCode() + "): " + body)))
                    .bodyToMono(String.class)
                    .block();
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