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
    private String seller_name;//판매자
    private String buyer_name;//구매자
    private Long seller_id;//판매자 아이디
    private Long buyer_id;//구매자 아이디
    private Product product;//상품 제목, 글, 가격 등
}
