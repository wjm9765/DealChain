package com.dealchain.dealchain.domain.contract.dto;

import com.dealchain.dealchain.domain.contract.entity.Contract;
import com.dealchain.dealchain.domain.contract.entity.SignTable;
import lombok.Builder;
import lombok.Getter;

@Getter
public class ContractInfoResponseDto {

    private String roomId;
    // contract객체의 Id (서명 완료 시)
    private Long contractId;
    // contract data 객체의 id (작성 중일 시)
    private Long contractDataId;
    private SignTable.SignStatus status;

    @Builder
    public ContractInfoResponseDto(String roomId, Long contractId, Long contractDataId, SignTable.SignStatus status) {
        this.roomId = roomId;
        this.contractId = contractId;
        this.contractDataId = contractDataId;
        this.status = status;
    }
}