package com.dealchain.dealchain.domain.AI.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Flask 탐지 API 응답을 매핑하는 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class detectDto {

    @JsonProperty("fraud_score")
    private Double fraud_score;

    @JsonProperty("fraud_type")
    private String fraud_type;

    @JsonProperty("message_id")
    private Long message_id;

    @JsonProperty("reason")
    private String reason;

}
