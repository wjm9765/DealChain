package com.dealchain.dealchain.domain.AI.service;

import com.dealchain.dealchain.domain.AI.dto.ContractDefaultReqeustDto;
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
    private final ContractDtoJsonConverter contractDtoJsonConverter;

    @Value("classpath:prompt/claude-contract-system-prompt.txt")
    private Resource systemPromptResource;

    @Value("classpath:prompt/claude-contract-reason-system-prompt.txt")
    private Resource systemPromptResource2;

    private String systemPrompt;
    private String systemReasonPrompt;

    public AICreateContract(BedrockRuntimeClient bedrockClient,
                            ContractDtoJsonConverter contractDtoJsonConverter
    ) {
        this.bedrockClient = bedrockClient;
        this.contractDtoJsonConverter = contractDtoJsonConverter;
    }

    @PostConstruct
    public void loadSystemPrompt() throws Exception {
        if (systemPromptResource == null || !systemPromptResource.exists() || systemPromptResource2 == null || !systemPromptResource2.exists()) {
            throw new IllegalStateException("System prompt resource not found. Place the files at `src/main/resources/prompt/claude-contract-system-prompt.txt` and `src/main/resources/prompt/claude-contract-reason-system-prompt.txt`");
        }

        try (InputStream is = systemPromptResource.getInputStream();
             InputStream is2 = systemPromptResource2.getInputStream()) {
            this.systemPrompt = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            this.systemReasonPrompt = new String(is2.readAllBytes(), StandardCharsets.UTF_8);
        }
    }


    public String invokeClaude(String userChatLog, ContractDefaultReqeustDto reqeustDto, String contract) {
        String defaultInfo = contractDtoJsonConverter.toJson(reqeustDto);
        String system;
        String content;

        if (contract != null) {
            String wrappedContract = "<contract>" + contract + "</contract>";
            system = this.systemReasonPrompt;
            content = (userChatLog == null ? "" : userChatLog) + wrappedContract + defaultInfo;
        } else {
            system = this.systemPrompt;
            content = (userChatLog == null ? "" : userChatLog) + defaultInfo;
        }

        return callClaudeWithSystem(system, content);
    }

    /**
     * 실제 Bedrock/Claude 호출을 담당하는 공통 메서드.
     */
    private String callClaudeWithSystem(String system, String content) {
        String modelId = "apac.anthropic.claude-3-sonnet-20240229-v1:0";

        JSONObject requestBody = new JSONObject();
        requestBody.put("anthropic_version", "bedrock-2023-05-31");
        requestBody.put("max_tokens", 2048);
        requestBody.put("system", system);

        JSONArray messages = new JSONArray();
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", content);
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
