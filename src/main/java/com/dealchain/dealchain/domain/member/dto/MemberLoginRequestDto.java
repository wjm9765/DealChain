package com.dealchain.dealchain.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MemberLoginRequestDto {
    
    @NotBlank(message = "아이디는 필수입니다.")
    private String id;
    
    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;
}
