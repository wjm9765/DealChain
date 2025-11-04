package com.dealchain.dealchain.domain.chat.service;

import com.dealchain.dealchain.domain.chat.dto.WebMessageDto;
import com.dealchain.dealchain.domain.chat.entity.ChatMessage;
import com.dealchain.dealchain.domain.chat.entity.ChatRoom;
import com.dealchain.dealchain.domain.chat.repository.ChatMessageRepository;
import com.dealchain.dealchain.domain.chat.repository.ChatRoomRepository;
import com.dealchain.dealchain.domain.security.XssSanitizer;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;

@Service
@RequiredArgsConstructor
public class WebChatService {
    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final XssSanitizer xssSanitizer;

    @Transactional
    public void handleChatMessage(WebMessageDto messageDto, Principal principal){
        String destination = "/sub/chat/room/" + messageDto.getRoomId();

        // 1. Room 조회
        String roomId = messageDto.getRoomId();
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("채팅방을 찾을 수 없습니다. ID: " + roomId));

        // 2. sender가 방 참가자인지 확인
        if (messageDto.getSenderId() == null) {
            throw new EntityNotFoundException("유효하지 않은 사용자입니다.");
        }
        Long seller = chatRoom.getSellerId();
        Long buyer = chatRoom.getBuyerId();
        if (!messageDto.getSenderId().equals(seller) && !messageDto.getSenderId().equals(buyer)) {
            throw new EntityNotFoundException("채팅방에 존재하지 않는 사용자입니다. ID=" + messageDto.getSenderId());
        }

        // 3. Principal(토큰)과 senderId 일치 여부 확인
        Long principalId = Long.valueOf(principal.getName());
        if (principal == null || !messageDto.getSenderId().equals(principalId)) {
            throw new EntityNotFoundException("토큰의 사용자와 senderId가 일치하지 않습니다."+principalId);
        }

        // 메시지 타입별 처리
        if (WebMessageDto.MessageType.TALK.equals(messageDto.getType())) {
            // XSS 방어: 채팅 메시지 살균
            String sanitized = xssSanitizer.sanitizeForChat(messageDto.getMessage());

            ChatMessage chatMessage = ChatMessage.builder()
                    .chatRoom(chatRoom)
                    .senderId(messageDto.getSenderId())
                    .content(sanitized)
                    .build();

            chatMessageRepository.save(chatMessage);
            messagingTemplate.convertAndSend(destination, messageDto);
        } else if (WebMessageDto.MessageType.LEAVE.equals(messageDto.getType())) {//대화 방 나갔다는 알림 로직
            messageDto.setMessage(messageDto.getSenderId() + "님이 퇴장하셨습니다.");
            messagingTemplate.convertAndSend(destination, messageDto);
        } else if( WebMessageDto.MessageType.ENTER.equals(messageDto.getType())){//대화 방 들어왔다는 알림 로직
            messageDto.setMessage(messageDto.getSenderId() + "님이 입장하셨습니다.");
            messagingTemplate.convertAndSend(destination, messageDto);
        }
    }
}
