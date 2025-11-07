package com.dealchain.dealchain.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class FlaskWebClientConfig {

    // application.properties에서 api.flask.base-url 값을 주입받습니다.
    @Value("${api.gateway.base-url}")
    private String baseUrl;

    @Bean
    public WebClient flaskApiWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl) // 1. 기본 URL 설정
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) // 2. JSON 통신 기본 설정
                // API 키나 복잡한 인증 헤더는 추가하지 않습니다.
                .build();
    }
}