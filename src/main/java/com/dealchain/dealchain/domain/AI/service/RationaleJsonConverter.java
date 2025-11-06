package com.dealchain.dealchain.domain.AI.service;

import com.dealchain.dealchain.domain.AI.dto.RationaleResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class RationaleJsonConverter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * JSON 문자열을 RationaleResponseDto로 변환
     * 
     * @param jsonString JSON 문자열
     * @return RationaleResponseDto 객체
     * @throws RuntimeException JSON 파싱 실패 시
     */
    public RationaleResponseDto fromJson(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            
            RationaleResponseDto.RationaleResponseDtoBuilder builder = RationaleResponseDto.builder();

            // rationale 변환
            if (json.has("rationale") && !json.isNull("rationale")) {
                JSONObject rationaleJson = json.getJSONObject("rationale");
                RationaleResponseDto.Rationale.RationaleBuilder rationaleBuilder = RationaleResponseDto.Rationale.builder();

                // reason 변환
                if (rationaleJson.has("reason") && !rationaleJson.isNull("reason")) {
                    JSONObject reasonJson = rationaleJson.getJSONObject("reason");
                    RationaleResponseDto.Rationale.Reason reason = RationaleResponseDto.Rationale.Reason.builder()
                            .item_details(reasonJson.optString("item_details", null))
                            .payment(reasonJson.optString("payment", null))
                            .delivery(reasonJson.optString("delivery", null))
                            .cancellation_policy(reasonJson.optString("cancellation_policy", null))
                            .contract_date(reasonJson.optString("contract_date", null))
                            .dispute_resolution(reasonJson.optString("dispute_resolution", null))
                            .escrow(reasonJson.optString("escrow", null))
                            .other_terms(reasonJson.optString("other_terms", null))
                            .parties(reasonJson.optString("parties", null))
                            .refund_policy(reasonJson.optString("refund_policy", null))
                            .build();
                    rationaleBuilder.reason(reason);
                }

                builder.rationale(rationaleBuilder.build());
            }

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException("JSON을 DTO로 변환 실패", e);
        }
    }

    /**
     * RationaleResponseDto를 JSON 문자열로 변환
     * 
     * @param dto RationaleResponseDto 객체
     * @return JSON 문자열
     * @throws RuntimeException JSON 변환 실패 시
     */
    public String toJson(RationaleResponseDto dto) {
        try {
            return MAPPER.writeValueAsString(dto);
        } catch (Exception e) {
            throw new RuntimeException("DTO를 JSON으로 변환 실패", e);
        }
    }
}

