// java
package com.dealchain.dealchain.domain.AI.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sagemakerruntime.SageMakerRuntimeClient;
import software.amazon.awssdk.services.sagemakerruntime.model.InvokeEndpointRequest;

import java.nio.charset.StandardCharsets;

@Service
public class SageMakerService {

    private final SageMakerRuntimeClient sageMakerClient;
    private final String endpointName;

    public SageMakerService(SageMakerRuntimeClient sageMakerClient,
                            @Value("${sagemaker.endpoint.name}") String endpointName) {
        this.sageMakerClient = sageMakerClient;
        this.endpointName = endpointName;
    }

    /**
     * 요청 JSON 문자열을 그대로 SageMaker로 전송하고 응답 JSON 문자열을 반환한다.
     * - 입력/출력 모두 이미 JSON 문자열이면 추가 직렬화/역직렬화 불필요
     * - 호출자에서 필요한 형태로 파싱해서 처리하도록 위임
     */
    public String invokeEndpoint(String requestJson) {
        InvokeEndpointRequest invokeRequest = InvokeEndpointRequest.builder()
                .endpointName(endpointName)
                .contentType("application/json")
                .accept("application/json")
                .body(SdkBytes.fromString(requestJson, StandardCharsets.UTF_8))
                .build();

        return sageMakerClient.invokeEndpoint(invokeRequest)
                .body()
                .asString(StandardCharsets.UTF_8);
    }
}
