package com.dealchain.dealchain.domain.chat.service;

import com.dealchain.dealchain.domain.chat.dto.WebMessageDto;
import com.dealchain.dealchain.domain.chat.entity.ChatMessage;
import com.dealchain.dealchain.domain.chat.entity.ChatRoom;
import com.dealchain.dealchain.domain.chat.repository.ChatMessageRepository;
import com.dealchain.dealchain.domain.chat.repository.ChatRoomRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WebChatService {
    private final SimpMessageSendingOperations messagingTemplate; //메시지 전송용
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public void handleChatMessage(WebMessageDto messageDto){
        String destination = "/sub/chat/room/" + messageDto.getRoomId();
        //message를 어디에 보낼지 저장

        //1. ChatRoom 존재여부 확인
        String roomId = messageDto.getRoomId();
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> {
                    System.out.println("채팅방을 찾을 수 없습니다. ID: " + messageDto.getRoomId());

                    return new EntityNotFoundException("채팅방을 찾을 수 없습니다. ID: " + messageDto.getRoomId());
                });
        //2. 채팅 보내는 사람 id가 채팅방에 존재하는지 확인?
        //예외 처리

        if (WebMessageDto.MessageType.TALK.equals(messageDto.getType())) {
            // TALK: 메시지 저장 및 브로드캐스트
            System.out.println("Talk 메시지 처리: RoomID=" + roomId + ", Sender=" + messageDto.getSenderId());

            // DTO -> Entity 변환
            ChatMessage chatMessage = ChatMessage.builder()
                    .chatRoom(chatRoom)
                    .senderId(messageDto.getSenderId())
                    .content(messageDto.getMessage())
                    // timestamp는 @PrePersist로 자동 생성됨
                    .build();

            // DB에 메시지 저장
            chatMessageRepository.save(chatMessage);

            // 해당 채팅방 구독자들에게 메시지 전송
            messagingTemplate.convertAndSend(destination, messageDto);

        } else if (WebMessageDto.MessageType.LEAVE.equals(messageDto.getType())) {
            // QUIT: 채팅방 삭제 및 퇴장 메시지 브로드캐스트
            System.out.println("Talk 메시지 처리: RoomID=" + roomId + ", Sender=" + messageDto.getSenderId());

            // 퇴장 메시지 설정 및 전송
            messageDto.setMessage(messageDto.getSenderId() + "님이 퇴장하셨습니다.");
            messagingTemplate.convertAndSend(destination, messageDto);

            // 채팅방 삭제?
            //chatRoomRepository.delete(chatRoom);
            //log.info("채팅방 삭제 완료: RoomID={}", roomId);

        }

    }
}
