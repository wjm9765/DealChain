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
public class ContractResponseDto {
    private Contract contract;

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Contract {
        private Parties parties;
        private ItemDetails item_details;
        private Payment payment;
        private Delivery delivery;
        private Escrow escrow;
        private CancellationPolicy cancellation_policy;
        private RefundPolicy refund_policy;
        private DisputeResolution dispute_resolution;
        private OtherTerms other_terms;
        private String contract_date;
        private String title;

        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Getter
        @Setter
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Parties {
            private PartyInfo buyer;
            private PartyInfo seller;

            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            @Getter
            @Setter
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class PartyInfo {
                private String address;
                private String name;
                private String phone;
            }
        }

        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Getter
        @Setter
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class ItemDetails {
            private String name;
            private String condition_and_info;
        }

        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Getter
        @Setter
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Payment {
            private String price;
            private String price_method;
            private String payment_method;
            private String payment_schedule;
        }

        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Getter
        @Setter
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Delivery {
            private String method;
            private String schedule;
        }

        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Getter
        @Setter
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Escrow {
            private String details;
        }

        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Getter
        @Setter
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class CancellationPolicy {
            private String details;
        }

        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Getter
        @Setter
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class RefundPolicy {
            private String details;
        }

        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Getter
        @Setter
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class DisputeResolution {
            private String details;
        }

        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Getter
        @Setter
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class OtherTerms {
            private String technical_specs;
            private String general_terms;
        }
    }
}

