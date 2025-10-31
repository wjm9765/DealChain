package com.dealchain.dealchain.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MemberRegisterRequestDto {
    
    @NotBlank(message = "이름은 필수입니다.")
    private String name;
    
    @NotBlank(message = "주민번호는 필수입니다.")
    @Pattern(regexp = "^\\d{6}-\\d{7}$", message = "주민번호는 000000-0000000 형태로 입력해주세요.")
    private String residentNumber;
    
    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "^\\d{3}-\\d{4}-\\d{4}$", message = "전화번호는 000-0000-0000 형태로 입력해주세요.")
    private String phoneNumber;
}
