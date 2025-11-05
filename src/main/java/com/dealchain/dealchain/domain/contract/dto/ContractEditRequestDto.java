package com.dealchain.dealchain.domain.contract.dto;

import lombok.*;
import org.springframework.stereotype.Component;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractEditRequestDto {
    private Long sellerId;
    private Long buyerId;
    private String roomId;
    private String deviceInfo;
    private String editjson;
}
