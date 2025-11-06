package com.dealchain.dealchain.domain.chat.service;

import com.dealchain.dealchain.domain.chat.dto.SQSrequestDto;
import com.dealchain.dealchain.domain.chat.dto.WebMessageDto;
import com.dealchain.dealchain.domain.chat.entity.ChatMessage;
import com.dealchain.dealchain.domain.chat.entity.ChatRoom;
import com.dealchain.dealchain.domain.chat.repository.ChatMessageRepository;
import com.dealchain.dealchain.domain.chat.repository.ChatRoomRepository;
import com.dealchain.dealchain.domain.security.XssSanitizer;
import com.fasterxml.jackson.databind.ObjectMapper; //JSON 직렬화
import io.awspring.cloud.sqs.operations.SqsTemplate; // SQS 전송
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;

@Slf4j
@Service
public class WebChatService {
    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final XssSanitizer xssSanitizer;

    // SQS 전송용
    private final SqsTemplate sqsTemplate;
    // DTO를 JSON으로 직렬화하기 위한 ObjectMapper 주입
    private final ObjectMapper objectMapper;

    // SQS 큐 이름
    private static final String FRAUD_DETECTION_QUEUE = "my-fraud-queue";

    //생성자 직접 구현
    public WebChatService(SimpMessageSendingOperations messagingTemplate,
                          ChatRoomRepository chatRoomRepository,
                          ChatMessageRepository chatMessageRepository,
                          XssSanitizer xssSanitizer,
                          SqsTemplate sqsTemplate,
                          ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.chatRoomRepository = chatRoomRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.xssSanitizer = xssSanitizer;
        this.sqsTemplate = sqsTemplate;
        this.objectMapper = objectMapper;
    }


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


        // 3. Principal(토큰)과 senderId 일치 여부 확인
        Long principalId = Long.valueOf(principal.getName());
        if (principal == null || !messageDto.getSenderId().equals(principalId)) {
            throw new EntityNotFoundException("토큰의 사용자와 senderId가 일치하지 않습니다."+principalId);
        }

        Long seller = chatRoom.getSellerId();
        Long buyer = chatRoom.getBuyerId();
        Long receiverId  = null;
        if (!messageDto.getSenderId().equals(seller) && !messageDto.getSenderId().equals(buyer)) {
            throw new EntityNotFoundException("채팅방에 존재하지 않는 사용자입니다. ID=" + messageDto.getSenderId());
        }
        else if(messageDto.getSenderId().equals(seller)){
            receiverId = buyer;
        }
        else if(messageDto.getSenderId().equals(buyer)){
            receiverId = seller;
        }



        // 메시지 타입별 처리
        if (WebMessageDto.MessageType.TALK.equals(messageDto.getType())) {
            // XSS 방어: 채팅 메시지 살균
            String sanitized = xssSanitizer.sanitizeForChat(messageDto.getMessage());

            //  살균된 메시지로 DTO를 업데이트 (SQS와 WebSocket에 동일하게 전송)
            messageDto.setMessage(sanitized);

            ChatMessage chatMessage = ChatMessage.builder()
                    .chatRoom(chatRoom)
                    .senderId(messageDto.getSenderId())
                    .content(sanitized)
                    .receiverId(receiverId)
                    .build();

            chatMessageRepository.save(chatMessage);

            // SQS로 비동기 사기 탐지 요청 전송
            SQSrequestDto sqSrequestDto = SQSrequestDto.builder()
                    .roomId(messageDto.getRoomId())
                    .senderId(messageDto.getSenderId())
                    .receiverId(chatMessage.getReceiverId())
                    .message(chatMessage.getContent())
                    .messageId(chatMessage.getMessageId())
                    .build();

            sendToFraudDetectionQueue(sqSrequestDto);

            messagingTemplate.convertAndSend(destination, messageDto);

        } else if (WebMessageDto.MessageType.LEAVE.equals(messageDto.getType())) {
            messageDto.setMessage(messageDto.getSenderId() + "님이 퇴장하셨습니다.");
            messagingTemplate.convertAndSend(destination, messageDto);
        } else if( WebMessageDto.MessageType.ENTER.equals(messageDto.getType())){
            messageDto.setMessage(messageDto.getSenderId() + "님이 입장하셨습니다.");
            messagingTemplate.convertAndSend(destination, messageDto);
        }
    }

    /**
     * [핵심] SQS에 메시지를 전송하는 별도 함수
     */
    private void sendToFraudDetectionQueue(SQSrequestDto messageDto) {
        try {
            // DTO 객체를 JSON 문자열로 변환 (AI Consumer가 파싱할 수 있도록)
            String jsonPayload = objectMapper.writeValueAsString(messageDto);

            // SQS 큐로 전송
            sqsTemplate.send(FRAUD_DETECTION_QUEUE, jsonPayload);

        } catch (Exception e) {
            // SQS 전송에 실패하더라도, 채팅은 성공해야 하므로 에러만 로깅
            log.warn("SQS 사기 탐지 큐 전송 실패 (채팅은 정상 처리됨). RoomId: {}, Error: {}",
                    messageDto.getRoomId(), e.getMessage());
        }
    }
}