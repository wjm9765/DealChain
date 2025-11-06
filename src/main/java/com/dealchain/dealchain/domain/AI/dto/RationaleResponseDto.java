package com.dealchain.dealchain.domain.AI.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RationaleResponseDto {
    private Rationale rationale;

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Rationale {
        private Reason reason;

        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Getter
        @Setter
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Reason {
            private String item_details;
            private String payment;
            private String delivery;
            private String cancellation_policy;
            private String contract_date;
            private String dispute_resolution;
            private String escrow;
            private String other_terms;
            private String parties;
            private String refund_policy;
        }
    }
}

