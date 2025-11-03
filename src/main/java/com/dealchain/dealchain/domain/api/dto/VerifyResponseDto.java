package com.dealchain.dealchain.domain.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VerifyResponseDto {
    private Boolean success;
    private String message;
}

