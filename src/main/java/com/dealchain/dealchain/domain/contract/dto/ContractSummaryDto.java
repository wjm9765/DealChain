package com.dealchain.dealchain.domain.contract.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@Getter
@NoArgsConstructor
public class ContractSummaryDto {
    private boolean isSuccess;
    private String summary;//계약서 요약본
    private String data;//오류 일시 정보 담아서 반환
}
