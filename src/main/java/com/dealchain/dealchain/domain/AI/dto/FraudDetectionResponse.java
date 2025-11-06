package com.dealchain.dealchain.domain.AI.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

// Lombok 어노테이션으로 Getter, Setter, 생성자 등을 자동 생성합니다.
@Data
@NoArgsConstructor // JSON 역직렬화를 위해 기본 생성자가 필요합니다.
public class FraudDetectionResponse {

    // JSON의 fraud_score 필드를 이 변수에 매핑
    @JsonProperty("fraud_score")
    private double fraudScore;

    // JSON의 fraud_type 필드를 이 변수에 매핑
    @JsonProperty("fraud_type")
    private String fraudType;

    // JSON의 message_id 필드를 이 변수에 매핑
    @JsonProperty("message_id")
    private Long messageId; // id가 null일 수 있으므로 String 또는 Integer 사용

    // JSON의 reason 필드를 이 변수에 매핑
    @JsonProperty("reason")
    private String reason;
}