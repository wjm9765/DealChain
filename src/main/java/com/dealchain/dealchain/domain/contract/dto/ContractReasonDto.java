package com.dealchain.dealchain.domain.contract.dto;

import com.dealchain.dealchain.domain.AI.dto.RationaleResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@Getter
@NoArgsConstructor
public class ContractReasonDto {
    private boolean isSuccess;
    private RationaleResponseDto rationaleResponseDto;

    private String data;//오류 일시 정보 담아서 반환

}
