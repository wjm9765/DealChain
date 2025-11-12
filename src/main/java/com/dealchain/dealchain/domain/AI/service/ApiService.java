// java
package com.dealchain.dealchain.domain.AI.service;

import com.dealchain.dealchain.domain.AI.dto.detectDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiService {
    private final WebClient flaskApiWebClient;

    public detectDto sendPostRequest(String test) {
        try {
            detectDto responsed = flaskApiWebClient.post()
                    .uri("/detect_fraud")
                    .bodyValue(test)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .map(body -> new RuntimeException("Flask API 오류 (" + response.statusCode() + "): " + body)))
                    .bodyToMono(detectDto.class)
                    .block();
            log.info("Flask API 응답 수신 성공.");
            return responsed;
        } catch (WebClientResponseException e) {
            log.error("Flask API 호출 실패 (상태 코드: {}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            return new detectDto(null, null, null,
                    "API 호출 실패: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Flask API 호출 중 일반 오류 발생: {}", e.getMessage());
            return new detectDto(null, null, null, "일반 오류: " + e.getMessage());
        }
    }
}
