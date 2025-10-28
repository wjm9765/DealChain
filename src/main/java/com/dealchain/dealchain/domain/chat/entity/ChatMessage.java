package com.dealchain.dealchain.domain.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "chat_message") // 테이블 이름 지정
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id",nullable = false)
    private Long messageId; // 메시지 고유 ID,랜덤 생성

    // ChatRoom 객체를 참조하여 chatroomID 역할을 함
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom chatRoom;

    // senderID 역할
    @Column(name ="sender_id",nullable = false)
    private String senderId;


    // text 역할
    @Column(columnDefinition = "TEXT")
    private String content;

    // 메세지 보낸 시간
    private LocalDateTime timestamp;


    @PrePersist
    public void prePersist() {
        this.timestamp = LocalDateTime.now(); // time 자동 설정
    }
}
