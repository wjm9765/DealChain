package com.dealchain.dealchain.domain.contract.dto;


import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
public class SignResponseDto {
    private boolean isSuccess;
    private String data;//오류 정보
    private boolean bothSign;
}
