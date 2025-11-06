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
    private Long senderId;


    //받는 사람
    @Column(name ="receiver_id",nullable = false)
    private Long receiverId;
    // text 역할
    @Column(columnDefinition = "TEXT")
    private String content;

    // 메세지 보낸 시간
    private LocalDateTime timestamp;


    @PrePersist
    public void prePersist() {
        this.timestamp = LocalDateTime.now(); // 서버기준 시간 설정
    }
}
