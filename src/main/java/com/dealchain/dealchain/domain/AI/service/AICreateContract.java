// java
package com.dealchain.dealchain.domain.AI.service;

import jakarta.annotation.PostConstruct;
import org.json.JSONArray;
import org.json.JSONObject;
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
public class AICreateContract {
    private final BedrockRuntimeClient bedrockClient;

    @Value("classpath:prompt/claude-contract-system-prompt.txt")
    private Resource systemPromptResource;

    private String systemPrompt;

    public AICreateContract(BedrockRuntimeClient bedrockClient) {
        this.bedrockClient = bedrockClient;
    }

    @PostConstruct//시스템 시작할 때 한번 시작
    public void loadSystemPrompt() throws Exception {
        if (systemPromptResource == null || !systemPromptResource.exists()) {
            //시스템 프롬프트가 없을 시
            throw new IllegalStateException("System prompt resource not found. Place the file at `src/main/resources/prompt/claude-contract-system-prompt.txt`");
        }

        try (InputStream is = systemPromptResource.getInputStream()) {
            //시스템 프롬포트 가져오기
            this.systemPrompt = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public String invokeClaude(String userChatLog) {
        String modelId = "apac.anthropic.claude-3-sonnet-20240229-v1:0";

        JSONObject requestBody = new JSONObject();
        requestBody.put("anthropic_version", "bedrock-2023-05-31");
        requestBody.put("max_tokens", 2048);
        requestBody.put("system", this.systemPrompt);

        JSONArray messages = new JSONArray();
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", userChatLog);
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
