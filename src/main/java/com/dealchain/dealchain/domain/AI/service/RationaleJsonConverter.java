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
     * * @param jsonString 원본 문자열 (텍스트가 섞여 있을 수 있음)
     * @return RationaleResponseDto 객체
     * @throws RuntimeException JSON 파싱 실패 시
     */
    public RationaleResponseDto fromJson(String jsonString) {
        try {

            String cleanJson = JsonExtractor.extractJsonBlock(jsonString);


            JSONObject json = new JSONObject(cleanJson);

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
        } catch (IllegalArgumentException e) {
            // JSON 블록 추출 실패 시 예외 처리
            throw new RuntimeException("JSON 블록 추출 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            // DTO 변환 중 구조적 오류 발생 시 예외 처리
            throw new RuntimeException("추출된 JSON을 DTO로 변환 실패", e);
        }
    }

    /**
     * RationaleResponseDto를 JSON 문자열로 변환 (기능 유지)
     * * @param dto RationaleResponseDto 객체
     * @return JSON 문자열
     * @throws RuntimeException JSON 변환 실패 시
     */
    public String toJson(RationaleResponseDto dto) {
        try {
            // 이 메서드는 DTO 객체를 입력받으므로, JSON 추출 로직이 필요 없습니다.
            return MAPPER.writeValueAsString(dto);
        } catch (Exception e) {
            throw new RuntimeException("DTO를 JSON으로 변환 실패", e);
        }
    }
}