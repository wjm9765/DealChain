package com.dealchain.dealchain.domain.contract.dto;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SignRequestDto
{
    @NotEmpty(message = "roomId는 필수입니다.") // [보안] 널(null)과 빈 문자열("") 모두 방어
    private String roomId;

    @NotNull(message = "productId는 필수입니다.") // [보안] 널(null) 방어
    private Long productId;

    @Column
    private String deviceInfo;
}
