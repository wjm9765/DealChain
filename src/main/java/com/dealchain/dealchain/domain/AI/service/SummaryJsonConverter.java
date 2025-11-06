package com.dealchain.dealchain.domain.AI.service;

import com.dealchain.dealchain.domain.AI.dto.SummaryResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class SummaryJsonConverter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * JSON 문자열을 SummaryResponseDto로 변환
     * 
     * @param jsonString JSON 문자열
     * @return SummaryResponseDto 객체
     * @throws RuntimeException JSON 파싱 실패 시
     */
    public SummaryResponseDto fromJson(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            
            SummaryResponseDto.SummaryResponseDtoBuilder builder = SummaryResponseDto.builder();

            // summary 변환
            if (json.has("summary") && !json.isNull("summary")) {
                JSONObject summaryJson = json.getJSONObject("summary");
                SummaryResponseDto.Summary summary = SummaryResponseDto.Summary.builder()
                        .final_summary(summaryJson.optString("final_summary", null))
                        .build();
                builder.summary(summary);
            }

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException("JSON을 DTO로 변환 실패", e);
        }
    }

    /**
     * SummaryResponseDto를 JSON 문자열로 변환
     * 
     * @param dto SummaryResponseDto 객체
     * @return JSON 문자열
     * @throws RuntimeException JSON 변환 실패 시
     */
    public String toJson(SummaryResponseDto dto) {
        try {
            return MAPPER.writeValueAsString(dto);
        } catch (Exception e) {
            throw new RuntimeException("DTO를 JSON으로 변환 실패", e);
        }
    }
}

