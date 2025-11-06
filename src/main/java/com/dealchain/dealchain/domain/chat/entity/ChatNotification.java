package com.dealchain.dealchain.domain.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "chat_notification") // 테이블 이름 지정
public class ChatNotification {

    public enum NotificationType {

        WARNING_FRAUD,      // (경고) AI 사기 탐지 경고
        CONTRACT_REQUEST,   // (요청) 계약서 생성 요청
        CONTRACT_REJECT,    // (거부) 계약서 서명 거부
        SIGN_REQUEST // (요청) 계약서 서명 요청
//        CONTRACT_EDITED,    // (알림) 계약서 수정됨
//        CONTRACT_COMPLETED  // (성공) 계약서 서명 완료
    }


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id; // 알림 고유 PK

    //누구한테 알림을 보낼지
    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    //누가 알림을 보냈는지
    @Column(name = "sender_id", nullable = false, updatable = false)
    private Long senderId;


    @Column(name = "room_id", nullable = false, updatable = false)
    private String roomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, updatable = false)
    private NotificationType type;

    // AI가 생성한 내용
    @Column(name = "ai_content", nullable = true, length = 512)
    private String AIContent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @Builder
    public ChatNotification(Long memberId, Long senderId,String roomId, NotificationType type, String AIContent) {
        if (memberId == null ||senderId==null|| roomId == null || type == null ) {
            throw new IllegalArgumentException("알림 생성에 필요한 필수 정보가 누락되었습니다.");
        }
        if (AIContent != null && AIContent.isBlank()) {
            throw new IllegalArgumentException("AIContent는 비어 있을 수 없습니다 (null은 가능).");
        }

        this.memberId = memberId;
        this.senderId = senderId;
        this.roomId = roomId;
        this.type = type;
        this.AIContent = AIContent;
    }

}