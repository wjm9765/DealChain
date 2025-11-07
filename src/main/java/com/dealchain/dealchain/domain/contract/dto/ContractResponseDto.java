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
public class ContractResponseDto {
    private boolean isSuccess;
    private com.dealchain.dealchain.domain.AI.dto.ContractResponseDto contractResponseDto;//json 형태로 계약서 데이터를 담음
    private String summary;//계약서 요약본
    private String data;//오류 일시 정보 담아서 반환


}
