package com.dealchain.dealchain.domain.AI.service;

import com.dealchain.dealchain.domain.AI.dto.ContractHelpResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class ContractHelpJsonConverter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * JSON 문자열을 ContractHelpResponseDto로 변환
     * 
     * @param jsonString JSON 문자열
     * @return ContractHelpResponseDto 객체
     * @throws RuntimeException JSON 파싱 실패 시
     */
    public ContractHelpResponseDto fromJson(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            
            ContractHelpResponseDto.ContractHelpResponseDtoBuilder builder = ContractHelpResponseDto.builder();

            // parties 변환
            if (json.has("parties") && !json.isNull("parties")) {
                JSONObject partiesJson = json.getJSONObject("parties");
                ContractHelpResponseDto.Parties parties = ContractHelpResponseDto.Parties.builder()
                        .seller(partiesJson.optString("seller", null))
                        .buyer(partiesJson.optString("buyer", null))
                        .build();
                builder.parties(parties);
            }

            // item_details 변환
            if (json.has("item_details") && !json.isNull("item_details")) {
                JSONObject itemDetailsJson = json.getJSONObject("item_details");
                ContractHelpResponseDto.ItemDetails itemDetails = ContractHelpResponseDto.ItemDetails.builder()
                        .name(itemDetailsJson.optString("name", null))
                        .condition_and_info(itemDetailsJson.optString("condition_and_info", null))
                        .build();
                builder.item_details(itemDetails);
            }

            // payment 변환
            if (json.has("payment") && !json.isNull("payment")) {
                JSONObject paymentJson = json.getJSONObject("payment");
                ContractHelpResponseDto.Payment payment = ContractHelpResponseDto.Payment.builder()
                        .price(paymentJson.optString("price", null))
                        .price_method(paymentJson.optString("price_method", null))
                        .payment_method(paymentJson.optString("payment_method", null))
                        .payment_schedule(paymentJson.optString("payment_schedule", null))
                        .build();
                builder.payment(payment);
            }

            // delivery 변환
            if (json.has("delivery") && !json.isNull("delivery")) {
                JSONObject deliveryJson = json.getJSONObject("delivery");
                ContractHelpResponseDto.Delivery delivery = ContractHelpResponseDto.Delivery.builder()
                        .method(deliveryJson.optString("method", null))
                        .schedule(deliveryJson.optString("schedule", null))
                        .build();
                builder.delivery(delivery);
            }

            // cancellation_policy 변환
            if (json.has("cancellation_policy") && !json.isNull("cancellation_policy")) {
                JSONObject cancellationPolicyJson = json.getJSONObject("cancellation_policy");
                ContractHelpResponseDto.CancellationPolicy cancellationPolicy = ContractHelpResponseDto.CancellationPolicy.builder()
                        .details(cancellationPolicyJson.optString("details", null))
                        .build();
                builder.cancellation_policy(cancellationPolicy);
            }

            // refund_policy 변환
            if (json.has("refund_policy") && !json.isNull("refund_policy")) {
                JSONObject refundPolicyJson = json.getJSONObject("refund_policy");
                ContractHelpResponseDto.RefundPolicy refundPolicy = ContractHelpResponseDto.RefundPolicy.builder()
                        .details(refundPolicyJson.optString("details", null))
                        .build();
                builder.refund_policy(refundPolicy);
            }

            // dispute_resolution 변환
            if (json.has("dispute_resolution") && !json.isNull("dispute_resolution")) {
                JSONObject disputeResolutionJson = json.getJSONObject("dispute_resolution");
                ContractHelpResponseDto.DisputeResolution disputeResolution = ContractHelpResponseDto.DisputeResolution.builder()
                        .details(disputeResolutionJson.optString("details", null))
                        .build();
                builder.dispute_resolution(disputeResolution);
            }

            // other_terms 변환
            if (json.has("other_terms") && !json.isNull("other_terms")) {
                JSONObject otherTermsJson = json.getJSONObject("other_terms");
                ContractHelpResponseDto.OtherTerms otherTerms = ContractHelpResponseDto.OtherTerms.builder()
                        .technical_specs(otherTermsJson.optString("technical_specs", null))
                        .general_terms(otherTermsJson.optString("general_terms", null))
                        .build();
                builder.other_terms(otherTerms);
            }

            // reason 변환
            if (json.has("reason") && !json.isNull("reason")) {
                JSONObject reasonJson = json.getJSONObject("reason");
                ContractHelpResponseDto.Reason reason = ContractHelpResponseDto.Reason.builder()
                        .item_details(reasonJson.optString("item_details", null))
                        .payment(reasonJson.optString("payment", null))
                        .delivery(reasonJson.optString("delivery", null))
                        .cancellation_policy(reasonJson.optString("cancellation_policy", null))
                        .build();
                builder.reason(reason);
            }

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException("JSON을 DTO로 변환 실패", e);
        }
    }

    /**
     * ContractHelpResponseDto를 JSON 문자열로 변환
     * 
     * @param dto ContractHelpResponseDto 객체
     * @return JSON 문자열
     * @throws RuntimeException JSON 변환 실패 시
     */
    public String toJson(ContractHelpResponseDto dto) {
        try {
            return MAPPER.writeValueAsString(dto);
        } catch (Exception e) {
            throw new RuntimeException("DTO를 JSON으로 변환 실패", e);
        }
    }
}

