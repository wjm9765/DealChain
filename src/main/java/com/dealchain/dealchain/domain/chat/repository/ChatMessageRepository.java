package com.dealchain.dealchain.domain.chat.repository;

import com.dealchain.dealchain.domain.chat.dto.ChatMessageDto;
import com.dealchain.dealchain.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatRoom_RoomIdOrderByTimestampAsc(String roomID);
}
