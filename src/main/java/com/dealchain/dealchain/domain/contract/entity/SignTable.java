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
@Table(name = "sign_table")
public class SignTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false, updatable = false)
    private String roomId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sign_status", nullable = false)
    private SignStatus status;

    @Column(name = "seller_signed_at", nullable = true)
    private LocalDateTime sellerSignedAt;

    @Column(name = "buyer_signed_at", nullable = true)
    private LocalDateTime buyerSignedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 서명 상태
     */
    public enum SignStatus {
        PENDING_BOTH,    // 양측 서명 대기
        PENDING_BUYER,   // 구매자 서명 대기 (판매자 완료)
        PENDING_SELLER,  // 판매자 서명 대기 (구매자 완료)
        COMPLETED        // 양측 서명 완료
    }

    @Builder
    public SignTable(String roomId, Long productId) {
        if (roomId == null || productId == null) {
            throw new IllegalArgumentException("Room ID와 Product ID는 필수입니다.");
        }
        this.roomId = roomId;
        this.productId = productId;
        this.status = SignStatus.PENDING_BOTH;
        this.sellerSignedAt = null;
        this.buyerSignedAt = null;
    }

    /**
     * 판매자 서명 처리
     * 이미 서명한 경우 중복 서명 방지
     */
    public void signBySeller() {
        if (this.sellerSignedAt == null) {
            this.sellerSignedAt = LocalDateTime.now();
            updateStatus();
        }
    }

    /**
     * 구매자 서명 처리
     * 이미 서명한 경우 중복 서명 방지
     */
    public void signByBuyer() {
        if (this.buyerSignedAt == null) {
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
            this.status = SignStatus.COMPLETED;
        } else if (sellerSigned) {
            this.status = SignStatus.PENDING_BUYER;
        } else if (buyerSigned) {
            this.status = SignStatus.PENDING_SELLER;
        } else {
            this.status = SignStatus.PENDING_BOTH;
        }
    }

    /**
     * 양측 서명 완료 여부 확인
     * @Transient: DB 컬럼이 아닌 계산된 값
     */
    @Transient
    public boolean isCompleted() {
        return this.status == SignStatus.COMPLETED;
    }
}