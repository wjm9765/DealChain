package com.dealchain.dealchain.domain.AI.service;

import com.dealchain.dealchain.domain.AI.dto.ContractResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class ContractJsonConverter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * JSON 문자열을 ContractResponseDto로 변환
     * 
     * @param jsonString JSON 문자열
     * @return ContractResponseDto 객체
     * @throws RuntimeException JSON 파싱 실패 시
     */
    public ContractResponseDto fromJson(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            
            ContractResponseDto.ContractResponseDtoBuilder builder = ContractResponseDto.builder();

            // contract 변환
            if (json.has("contract") && !json.isNull("contract")) {
                JSONObject contractJson = json.getJSONObject("contract");
                ContractResponseDto.Contract.ContractBuilder contractBuilder = ContractResponseDto.Contract.builder();

                // parties 변환
                if (contractJson.has("parties") && !contractJson.isNull("parties")) {
                    JSONObject partiesJson = contractJson.getJSONObject("parties");
                    ContractResponseDto.Contract.Parties.PartiesBuilder partiesBuilder = ContractResponseDto.Contract.Parties.builder();
                    
                    // buyer 변환
                    if (partiesJson.has("buyer") && !partiesJson.isNull("buyer")) {
                        JSONObject buyerJson = partiesJson.getJSONObject("buyer");
                        ContractResponseDto.Contract.Parties.PartyInfo buyer = ContractResponseDto.Contract.Parties.PartyInfo.builder()
                                .address(buyerJson.optString("address", null))
                                .name(buyerJson.optString("name", null))
                                .phone(buyerJson.optString("phone", null))
                                .build();
                        partiesBuilder.buyer(buyer);
                    }
                    
                    // seller 변환
                    if (partiesJson.has("seller") && !partiesJson.isNull("seller")) {
                        JSONObject sellerJson = partiesJson.getJSONObject("seller");
                        ContractResponseDto.Contract.Parties.PartyInfo seller = ContractResponseDto.Contract.Parties.PartyInfo.builder()
                                .address(sellerJson.optString("address", null))
                                .name(sellerJson.optString("name", null))
                                .phone(sellerJson.optString("phone", null))
                                .build();
                        partiesBuilder.seller(seller);
                    }
                    
                    contractBuilder.parties(partiesBuilder.build());
                }

                // item_details 변환
                if (contractJson.has("item_details") && !contractJson.isNull("item_details")) {
                    JSONObject itemDetailsJson = contractJson.getJSONObject("item_details");
                    ContractResponseDto.Contract.ItemDetails itemDetails = ContractResponseDto.Contract.ItemDetails.builder()
                            .name(itemDetailsJson.optString("name", null))
                            .condition_and_info(itemDetailsJson.optString("condition_and_info", null))
                            .build();
                    contractBuilder.item_details(itemDetails);
                }

                // payment 변환
                if (contractJson.has("payment") && !contractJson.isNull("payment")) {
                    JSONObject paymentJson = contractJson.getJSONObject("payment");
                    ContractResponseDto.Contract.Payment payment = ContractResponseDto.Contract.Payment.builder()
                            .price(paymentJson.optString("price", null))
                            .price_method(paymentJson.optString("price_method", null))
                            .payment_method(paymentJson.optString("payment_method", null))
                            .payment_schedule(paymentJson.optString("payment_schedule", null))
                            .build();
                    contractBuilder.payment(payment);
                }

                // delivery 변환
                if (contractJson.has("delivery") && !contractJson.isNull("delivery")) {
                    JSONObject deliveryJson = contractJson.getJSONObject("delivery");
                    ContractResponseDto.Contract.Delivery delivery = ContractResponseDto.Contract.Delivery.builder()
                            .method(deliveryJson.optString("method", null))
                            .schedule(deliveryJson.optString("schedule", null))
                            .build();
                    contractBuilder.delivery(delivery);
                }

                // escrow 변환
                if (contractJson.has("escrow") && !contractJson.isNull("escrow")) {
                    JSONObject escrowJson = contractJson.getJSONObject("escrow");
                    ContractResponseDto.Contract.Escrow escrow = ContractResponseDto.Contract.Escrow.builder()
                            .details(escrowJson.optString("details", null))
                            .build();
                    contractBuilder.escrow(escrow);
                }

                // cancellation_policy 변환
                if (contractJson.has("cancellation_policy") && !contractJson.isNull("cancellation_policy")) {
                    JSONObject cancellationPolicyJson = contractJson.getJSONObject("cancellation_policy");
                    ContractResponseDto.Contract.CancellationPolicy cancellationPolicy = ContractResponseDto.Contract.CancellationPolicy.builder()
                            .details(cancellationPolicyJson.optString("details", null))
                            .build();
                    contractBuilder.cancellation_policy(cancellationPolicy);
                }

                // refund_policy 변환
                if (contractJson.has("refund_policy") && !contractJson.isNull("refund_policy")) {
                    JSONObject refundPolicyJson = contractJson.getJSONObject("refund_policy");
                    ContractResponseDto.Contract.RefundPolicy refundPolicy = ContractResponseDto.Contract.RefundPolicy.builder()
                            .details(refundPolicyJson.optString("details", null))
                            .build();
                    contractBuilder.refund_policy(refundPolicy);
                }

                // dispute_resolution 변환
                if (contractJson.has("dispute_resolution") && !contractJson.isNull("dispute_resolution")) {
                    JSONObject disputeResolutionJson = contractJson.getJSONObject("dispute_resolution");
                    ContractResponseDto.Contract.DisputeResolution disputeResolution = ContractResponseDto.Contract.DisputeResolution.builder()
                            .details(disputeResolutionJson.optString("details", null))
                            .build();
                    contractBuilder.dispute_resolution(disputeResolution);
                }

                // other_terms 변환
                if (contractJson.has("other_terms") && !contractJson.isNull("other_terms")) {
                    JSONObject otherTermsJson = contractJson.getJSONObject("other_terms");
                    ContractResponseDto.Contract.OtherTerms otherTerms = ContractResponseDto.Contract.OtherTerms.builder()
                            .technical_specs(otherTermsJson.optString("technical_specs", null))
                            .general_terms(otherTermsJson.optString("general_terms", null))
                            .build();
                    contractBuilder.other_terms(otherTerms);
                }

                // contract_date 변환
                if (contractJson.has("contract_date") && !contractJson.isNull("contract_date")) {
                    contractBuilder.contract_date(contractJson.optString("contract_date", null));
                }

                // title 변환
                if (contractJson.has("title") && !contractJson.isNull("title")) {
                    contractBuilder.title(contractJson.optString("title", null));
                }

                builder.contract(contractBuilder.build());
            }

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException("JSON을 DTO로 변환 실패", e);
        }
    }

    /**
     * ContractResponseDto를 JSON 문자열로 변환
     * 
     * @param dto ContractResponseDto 객체
     * @return JSON 문자열
     * @throws RuntimeException JSON 변환 실패 시
     */
    public String toJson(ContractResponseDto dto) {
        try {
            return MAPPER.writeValueAsString(dto);
        } catch (Exception e) {
            throw new RuntimeException("DTO를 JSON으로 변환 실패", e);
        }
    }
}

