package com.dealchain.dealchain.domain.chat.service;

import com.dealchain.dealchain.domain.chat.dto.*;
import com.dealchain.dealchain.domain.chat.entity.ChatMessage;
import com.dealchain.dealchain.domain.chat.entity.ChatRoom;
import com.dealchain.dealchain.domain.chat.repository.ChatMessageRepository;
import com.dealchain.dealchain.domain.chat.repository.ChatRoomRepository;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class APIChatService {
    private static final Logger log = LoggerFactory.getLogger(APIChatService.class);

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;


    /**
     * 채팅방 생성 (중복 방지)
     */
    @Transactional
    public ChatRoomResponseDto createChatRoom(ChatRoomRequestDto requestDto) {
        // 같은 판매자-구매자 쌍의 채팅방 중복 생성 방지
        Optional<ChatRoom> existingRoomOpt = chatRoomRepository.findBySellerIdAndBuyerId(requestDto.getSeller(), requestDto.getBuyer());
        if(existingRoomOpt.isPresent()){
            ChatRoom existingRoom = existingRoomOpt.get();
            return new ChatRoomResponseDto(existingRoom.getRoomId(),false,"이미 존재하는 채팅방입니다.");
        }
        ChatRoom newRoom = ChatRoom.create(
                requestDto.getSeller(),
                requestDto.getBuyer(),
                requestDto.getProductId()
        );
        try {
            chatRoomRepository.save(newRoom);
            log.info("채팅방 생성 성공: {}", newRoom.getRoomId());
            return new ChatRoomResponseDto(newRoom.getRoomId(),true,null);
        } catch (Exception e) {
            // 보안: 구체적인 오류 내용을 노출하지 않음
            return new ChatRoomResponseDto(null,false, "채팅방 생성 중 오류가 발생했습니다.");
        }
    }

    @Transactional(readOnly = true)
    public ChatMessageRessponseDto getMessage(ChatMessageRequestDto requestDto) {
        Optional<ChatRoom> existingRoomOpt = chatRoomRepository.findBySellerIdAndBuyerId(requestDto.getSeller(),requestDto.getBuyer());

        if(existingRoomOpt.isEmpty()){
            return new ChatMessageRessponseDto(null,null,false);
        }

        String roomId = existingRoomOpt.get().getRoomId();
        List<ChatMessage> messages = chatMessageRepository.findByChatRoom_RoomIdOrderByTimestampAsc(roomId);

        List<ChatMessageDto> messageDtos = messages.stream()
                .map(msg -> new ChatMessageDto(
                        msg.getMessageId(),
                        msg.getSenderId(),
                        msg.getContent(),
                        msg.getTimestamp()
                )).collect(Collectors.toList());

        return new ChatMessageRessponseDto(roomId,messageDtos,true);
    }

    @Transactional(readOnly = true)
    public ChatRoomListResponseDto getChatRooms(Long userId) {
        List<ChatRoom> rooms = chatRoomRepository.findBySellerIdOrBuyerId(userId, userId);

        if (rooms == null || rooms.isEmpty()) {
            return new ChatRoomListResponseDto(List.of(), false, "채팅방이 존재하지 않습니다.");
        }

        List<chatRoomDto> dtos = rooms.stream()
                .map(r -> new chatRoomDto(r.getRoomId(), r.getSellerId(), r.getBuyerId(),r.getProductId()))
                .collect(Collectors.toList());

        return new ChatRoomListResponseDto(dtos, true, null);
    }

}
