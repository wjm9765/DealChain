package com.dealchain.dealchain.domain.contract.entity;

import jakarta.persistence.*; // Spring Boot 3.x+ (JPA 3.x)
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "sign_table") // 테이블 이름 지정
public class SignTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Primary Key

    //roomId와 productId는 변경 불가
    @Column(name = "room_id", nullable = false, updatable = false)
    private String roomId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private Long productId;

    //EnumType 로 현재 서명 상태 저장
    @Enumerated(EnumType.STRING) // DB에 "COMPLETED" 같은 문자열로 저장
    @Column(name = "sign_status", nullable = false)
    private SignStatus status;

    //null이면 서명하지 않음
    @Column(name = "seller_signed_at", nullable = true)
    private LocalDateTime sellerSignedAt;

    @Column(name = "buyer_signed_at", nullable = true)
    private LocalDateTime buyerSignedAt;

    // 생성 시간 자동 기록
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    // --- 서명 상태를 정의하는 Enum ---
    public enum SignStatus {
        PENDING_BOTH,    // 1. 양측 서명 대기
        PENDING_BUYER,   // 2. 구매자 서명 대기 (판매자 완료)
        PENDING_SELLER,  // 3. 판매자 서명 대기 (구매자 완료)
        COMPLETED        // 4. 양측 서명 완료 (both_sign = true)
    }



    //생성자
    @Builder
    public SignTable(String roomId, Long productId) {
        if (roomId == null || productId == null) {
            throw new IllegalArgumentException("Room ID와 Product ID는 필수입니다.");
        }
        this.roomId = roomId;
        this.productId = productId;

        // [초기 상태]
        this.status = SignStatus.PENDING_BOTH; // 1. 양측 서명 대기
        this.sellerSignedAt = null;
        this.buyerSignedAt = null;
    }


    //판매자가 서명하는 함수
    public void signBySeller() {
        if (this.sellerSignedAt == null) { // 이미 서명했으면 다시 안 함
            this.sellerSignedAt = LocalDateTime.now();
            updateStatus();
        }
    }

    //구매자가 서명하는 함수
    public void signByBuyer() {
        if (this.buyerSignedAt == null) { // 이미 서명했으면 다시 안 함
            this.buyerSignedAt = LocalDateTime.now();
            updateStatus();
        }
    }

    //수정 요청했을 때, 판매자/구매자 서명 취소 함수
    public void undoSignBySeller() {
        if (this.sellerSignedAt != null) {
            this.sellerSignedAt = null;
            this.status = SignStatus.PENDING_BOTH;
        }
    }
    public void undoSignByBuyer() {
        if (this.buyerSignedAt != null) {
            this.buyerSignedAt = null;
            this.status = SignStatus.PENDING_BOTH;
        }
    }

    //서명 상태 업데이트 함수
    private void updateStatus() {
        boolean sellerSigned = (this.sellerSignedAt != null);
        boolean buyerSigned = (this.buyerSignedAt != null);

        if (sellerSigned && buyerSigned) {
            this.status = SignStatus.COMPLETED; // 4. 양측 서명 완료
        } else if (sellerSigned) {
            this.status = SignStatus.PENDING_BUYER; // 2. 구매자 대기
        } else if (buyerSigned) {
            this.status = SignStatus.PENDING_SELLER; // 3. 판매자 대기
        } else {
            this.status = SignStatus.PENDING_BOTH; // 1. 양측 대기
        }
    }

    //서명 완료 여부 확인 함수
    @Transient // (DB 컬럼이 아님을 명시)
    public boolean isCompleted() {
        return this.status == SignStatus.COMPLETED;
    }
}