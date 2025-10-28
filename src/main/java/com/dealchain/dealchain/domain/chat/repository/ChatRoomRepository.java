package com.dealchain.dealchain.domain.chat.repository;

import com.dealchain.dealchain.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {
    //Seller,Buyer 순서에 맞춰 이미 방이 존재하는지 탐색
    //해당 채팅방이 있으면 optional에 담아 반환, 없다면 empty 반환
    Optional <ChatRoom> findBySellerIdAndBuyerId(String sellerId, String buyerId);
}