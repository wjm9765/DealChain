package com.dealchain.dealchain.domain.contract.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@Getter
@NoArgsConstructor
public class ContractResponseDto {
    private boolean isSuccess;
    private String data;//json 형태로 계약서 데이터를 담음
    private String summary;//계약서 요약본


}
