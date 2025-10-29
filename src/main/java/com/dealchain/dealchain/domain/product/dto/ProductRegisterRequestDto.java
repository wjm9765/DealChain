package com.dealchain.dealchain.domain.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Max;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductRegisterRequestDto {
    
    @NotBlank(message = "상품명은 필수입니다.")
    private String productName;
    
    @NotNull(message = "가격은 필수입니다.")
    @PositiveOrZero(message = "가격은 0 이상이어야 합니다.")
    @Max(value = 100000000, message = "가격은 100,000,000 이하여야 합니다.")
    private Long price;
    
    @NotBlank(message = "상품 설명은 필수입니다.")
    private String description;
}
