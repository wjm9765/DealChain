package com.dealchain.dealchain.domain.chat.service;

import com.dealchain.dealchain.domain.chat.dto.*;
import com.dealchain.dealchain.domain.chat.entity.ChatMessage;
import com.dealchain.dealchain.domain.chat.entity.ChatRoom;
import com.dealchain.dealchain.domain.chat.repository.ChatMessageRepository;
import com.dealchain.dealchain.domain.chat.repository.ChatRoomRepository;

import com.dealchain.dealchain.domain.product.Product;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
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
        //현재 로그인한 토큰으로 인증
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || authentication.getName() == null){
            return new ChatRoomResponseDto(null,false,"토큰이 존재하지 않습니다.");
        }
        String authName = authentication.getName();
        if (authName == null
                || (!authName.equals(String.valueOf(requestDto.getSeller()))
                && !authName.equals(String.valueOf(requestDto.getBuyer())))) {
            return new ChatRoomResponseDto(null, false,
                    "토큰의 사용자 ID가 요청과 일치하지 않습니다: " + authName);
        }
        //중복 방지

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
    public ChatMessageRessponseDto getMessage(ChatMessageRequestDto requestDto, Long principalId) {
        // principalId null 체크
        if (principalId == null) {
            return new ChatMessageRessponseDto(null, null, false);
        }
        // seller/buyer가 null인 경우 방 존재 여부 확인 불가
        if (requestDto.getSeller() == null || requestDto.getBuyer() == null) {
            return new ChatMessageRessponseDto(null, null, false);
        }
        // principal이 seller 또는 buyer인지 확인 (Long 안전 비교)
        if (!Objects.equals(principalId, requestDto.getSeller()) && !Objects.equals(principalId, requestDto.getBuyer())) {
            return new ChatMessageRessponseDto(null, null, false);
        }
        // seller/buyer로 채팅방 조회
        Optional<ChatRoom> existingRoomOpt = chatRoomRepository.findBySellerIdAndBuyerId(requestDto.getSeller(), requestDto.getBuyer());
        if (existingRoomOpt.isEmpty()) {
            return new ChatMessageRessponseDto(null, null, false);
        }

        ChatRoom room = existingRoomOpt.get();

        // 요청에 roomId가 포함되어 있으면 실제 roomId와 일치하는지 검증
        if (requestDto.getRoomId() != null && !requestDto.getRoomId().equals(room.getRoomId())) {
            return new ChatMessageRessponseDto(null, null, false);
        }

        // 조회된 방의 seller/buyer에 principal이 실제로 포함되어 있는지 확인
        if (!Objects.equals(principalId, room.getSellerId()) && !Objects.equals(principalId, room.getBuyerId())) {
            return new ChatMessageRessponseDto(null, null, false);
        }

        String roomId = room.getRoomId();
        List<ChatMessage> messages = chatMessageRepository.findByChatRoom_RoomIdOrderByTimestampAsc(roomId);

        List<ChatMessageDto> messageDtos = messages.stream()
                .map(msg -> new ChatMessageDto(
                        msg.getMessageId(),
                        msg.getSenderId(),
                        msg.getContent(),
                        msg.getTimestamp()
                )).collect(Collectors.toList());

        return new ChatMessageRessponseDto(roomId, messageDtos, true);
    }

    @Transactional(readOnly = true)
    public ChatRoomListResponseDto getChatRooms(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || authentication.getName() == null){
            return new ChatRoomListResponseDto(null,false,"토큰이 존재하지 않습니다.");
        }
        String authName = authentication.getName();
        if(!authName.equals(userId)){
            return new ChatRoomListResponseDto(null,false,
                    "토큰의 사용자 ID가 요청과 일치하지 않습니다: ");
        }
        //중복 방지

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
