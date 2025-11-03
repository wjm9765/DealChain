package com.dealchain.dealchain.domain.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyRequestDto {
    private String name;
    private String phoneNumber;
    private String residentNumber;
}

