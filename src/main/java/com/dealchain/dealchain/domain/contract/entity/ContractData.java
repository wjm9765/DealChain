package com.dealchain.dealchain.domain.contract.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "contract_data", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"roomId"})
})
public class ContractData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private String roomId;

    @Column(nullable = false, updatable = false)
    private Long sellerId;

    @Column(nullable = false, updatable = false)
    private Long buyerId;

    //저장할 때 암호화해서 저장
    @Lob // Large Object (TEXT 또는 CLOB 타입으로 매핑)
    @Column(name = "contract_json_data", nullable = false)
    private String contractJsonData;

    @Builder
    public ContractData(String roomId, Long sellerId, Long buyerId, String contractJsonData) {
        if (roomId == null || sellerId == null || buyerId == null || contractJsonData == null) {
            throw new IllegalArgumentException("필수 필드(roomId, sellerId, buyerId, contractJsonData)가 누락되었습니다.");
        }
        this.roomId = roomId;
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.contractJsonData = contractJsonData;
    }

    // (계약서 수정 편의 메서드)
    public void updateContractJson(String newJsonData) {
        this.contractJsonData = newJsonData;
    }
}