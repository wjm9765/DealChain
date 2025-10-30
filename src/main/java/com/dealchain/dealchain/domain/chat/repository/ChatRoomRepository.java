package com.dealchain.dealchain.domain.chat.repository;

import com.dealchain.dealchain.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {
    Optional<ChatRoom> findBySellerIdAndBuyerId(Long sellerId, Long buyerId);
}
