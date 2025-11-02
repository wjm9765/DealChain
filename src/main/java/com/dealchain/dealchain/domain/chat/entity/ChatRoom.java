package com.dealchain.dealchain.domain.chat.entity;


import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor //JPA 기본 생성자
@AllArgsConstructor // 모든 필드를 받는 생성자
@Builder // 빌더 패턴 지원
@Table(name = "chat_room") // 테이블 이름 지정
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // DB가 ID를 자동으로 생성하도록 설정
    @Column(name = "room_id",nullable = false)
    private String roomId;

    @Column(name = "seller_id",nullable = false)
    private Long sellerId; // 판매자 ID

    @Column(name = "buyer_id",nullable = false)
    private Long buyerId; // 구매자 ID


    //상품 id 추가
    @Column(name = "product_id", nullable = false)
    private Long productId; // 상품 ID

    // ChatMessage와 1:N 관계, 방 번호로 채팅 내용 조회
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();


    //서비스에서 생성자 활용
    public static ChatRoom create(Long sellerId, Long buyerId,Long productId) {
        ChatRoom room = new ChatRoom();
        room.sellerId = sellerId;
        room.buyerId = buyerId;
        room.productId = productId;
        return room;
    }
}
