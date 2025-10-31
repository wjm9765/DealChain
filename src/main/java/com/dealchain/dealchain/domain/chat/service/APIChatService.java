package com.dealchain.dealchain.domain.chat.service;

import com.dealchain.dealchain.domain.chat.dto.*;
import com.dealchain.dealchain.domain.chat.entity.ChatMessage;
import com.dealchain.dealchain.domain.chat.entity.ChatRoom;
import com.dealchain.dealchain.domain.chat.repository.ChatMessageRepository;
import com.dealchain.dealchain.domain.chat.repository.ChatRoomRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor//final 필드에 대한 생성자 자동 주입
public class APIChatService {


    private final ChatRoomRepository chatRoomRepository;
    //채팅방 db 연결
    private final ChatMessageRepository chatMessageRepository;


    @Transactional
    public ChatRoomResponseDto createChatRoom(ChatRoomRequestDto requestDto) {
        //같은 채팅방 중복 생성 방지 로직 필요
        Optional<ChatRoom> existingRoomOpt = chatRoomRepository.findBySellerIdAndBuyerId(requestDto.getSeller(), requestDto.getBuyer());
        if(existingRoomOpt.isPresent()){
            ChatRoom existingRoom = existingRoomOpt.get();
            return new ChatRoomResponseDto(existingRoom.getRoomId(),false,"이미 존재하는 채팅방입니다.");
            //기존에 존재하는 채팅방  id, false, 이유 반환
        }
        ChatRoom newRoom = ChatRoom.create(
                requestDto.getSeller(),
                requestDto.getBuyer()
        );
        try{//채팅방 생성 성공
            chatRoomRepository.save(newRoom);

            System.out.println("채팅방 생성 성공: " + newRoom.getRoomId());
            return new ChatRoomResponseDto(newRoom.getRoomId(),true,null);
            //채팅방 id, true, null 반환
        }
        catch (Exception e){//채팅방 생성 실패, 그 이유는 따로 저장 false Return
            //오류를 그대로 반환하는 것은 보안 취약점
            return new ChatRoomResponseDto(null,false, "채팅방 생성 중 오류가 발생했습니다.");
        }
    }

    @Transactional(readOnly = true)
    public ChatMessageRessponseDto getMessage(ChatMessageRequestDto requestDto) {
        Optional<ChatRoom> existingRoomOpt = chatRoomRepository.findBySellerIdAndBuyerId(requestDto.getSeller(),requestDto.getBuyer());

        //채팅방이 존재하지 않는 경우
        if(existingRoomOpt.isEmpty()){
            return new ChatMessageRessponseDto(null,null,false);
        }

        String roomId = existingRoomOpt.get().getRoomId();
        //존재하는 채팅방 번호로 각 메시지를 시간 순서대로 조회
        List<ChatMessage> messages = chatMessageRepository.findByChatRoom_RoomIdOrderByTimestampAsc(roomId);

        //chatmessage 를 dto로 변환
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

        //채팅방이하나도 없다면
        if (rooms == null || rooms.isEmpty()) {
            return new ChatRoomListResponseDto(List.of(), false, "채팅방이 존재하지 않습니다.");
        }

        List<chatRoomDto> dtos = rooms.stream()
                .map(r -> new chatRoomDto(r.getRoomId(), r.getSellerId(), r.getBuyerId()))
                .collect(Collectors.toList());

        return new ChatRoomListResponseDto(dtos, true, null);
    }

}
