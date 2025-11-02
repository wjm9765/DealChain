package com.dealchain.dealchain.domain.AI.dto;

import com.dealchain.dealchain.domain.product.Product;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContractDefaultReqeustDto {
    private Long sellerId;//판매자
    private Long buyerId;//구매자
    private Product product;//상품 제목, 글, 가격 등
}
