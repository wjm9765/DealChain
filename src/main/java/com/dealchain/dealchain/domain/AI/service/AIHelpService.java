package com.dealchain.dealchain.domain.AI.service;

import jakarta.annotation.PostConstruct;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger; 
import org.slf4j.LoggerFactory; 
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class AIHelpService {

    // [보안] 3. DoS 방어를 위한 최대 JSON 크기 (예: 5MB)
    private static final int MAX_JSON_SIZE = 5_242_880;
    private static final Logger log = LoggerFactory.getLogger(AIHelpService.class);

    private final BedrockRuntimeClient bedrockClient;

    @Value("classpath:prompt/claude-contract-help-system-prompt.txt")
    private Resource systemPromptResource;

    private String systemPrompt;

    public AIHelpService(BedrockRuntimeClient bedrockClient) {
        this.bedrockClient = bedrockClient;

    }

    @PostConstruct//시스템 시작할 때 한번 시작
    public void loadSystemPrompt() throws Exception {
        if (systemPromptResource == null || !systemPromptResource.exists()) {

            log.error("오류: AI 도움말 시스템 프롬프트 파일을 찾을 수 없습니다. (경로: {})", "src/main/resources/prompt/claude-contract-help-system-prompt.txt");
            throw new IllegalStateException("System prompt resource not found: claude-contract-help-system-prompt.txt");
        }

        try (InputStream is = systemPromptResource.getInputStream()) {
            //시스템 프롬포트 가져오기
            this.systemPrompt = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public String invokeClaude(String contractJsonInput) {

        if (contractJsonInput == null || contractJsonInput.length() > MAX_JSON_SIZE) {
            log.error("DoS 공격 의심: AI JSON 크기가 {}바이트를 초과했습니다. (Size: {})",
                    MAX_JSON_SIZE, (contractJsonInput == null ? 0 : contractJsonInput.length()));
            throw new IllegalArgumentException("AI가 생성한 계약서 데이터가 너무 큽니다.");
        }

        String modelId = "apac.anthropic.claude-3-sonnet-20240229-v1:0";

        JSONObject requestBody = new JSONObject();
        requestBody.put("anthropic_version", "bedrock-2023-05-31");
        requestBody.put("max_tokens", 2048);
        requestBody.put("system", this.systemPrompt);

        JSONArray messages = new JSONArray();
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");

        userMessage.put("content", contractJsonInput);

        messages.put(userMessage);

        requestBody.put("messages", messages);

        InvokeModelRequest request = InvokeModelRequest.builder()
                .modelId(modelId)
                .contentType("application/json")
                .accept("application/json")
                .body(SdkBytes.fromUtf8String(requestBody.toString()))
                .build();

        InvokeModelResponse response = bedrockClient.invokeModel(request);

        String responseBody = response.body().asUtf8String();
        JSONObject responseJson = new JSONObject(responseBody);

        return responseJson.getJSONArray("content")
                .getJSONObject(0)
                .getString("text");
    }
}