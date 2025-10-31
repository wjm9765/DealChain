package com.dealchain.dealchain.domain.chat.repository;

import com.dealchain.dealchain.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {
    // java
    List<ChatRoom> findBySellerIdOrBuyerId(Long sellerId, Long buyerId);
    Optional<ChatRoom> findBySellerIdAndBuyerId(Long sellerId, Long buyerId);
}
