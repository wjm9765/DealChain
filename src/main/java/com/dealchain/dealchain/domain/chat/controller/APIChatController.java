package com.dealchain.dealchain.domain.chat.controller;

import com.dealchain.dealchain.domain.chat.dto.ChatMessageRequestDto;
import com.dealchain.dealchain.domain.chat.dto.ChatMessageRessponseDto;
import com.dealchain.dealchain.domain.chat.dto.ChatRoomRequestDto;
import com.dealchain.dealchain.domain.chat.dto.ChatRoomResponseDto;
import com.dealchain.dealchain.domain.chat.service.APIChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class APIChatController {
    private final APIChatService chatService;

    //채팅방 생성
    @PostMapping("/createroom")
    public ResponseEntity<ChatRoomResponseDto> createChatRoom(
            @RequestBody ChatRoomRequestDto requestDto
    ) {
        ChatRoomResponseDto response = chatService.createChatRoom(requestDto);
        return ResponseEntity.ok(response);
    }

    //채팅 기록 조회
    @PostMapping("/getmessages")
    public ResponseEntity<ChatMessageRessponseDto> getChatMessages(
            @RequestBody ChatMessageRequestDto requestDto
    ) {
        ChatMessageRessponseDto response = chatService.getMessage(requestDto);
        return ResponseEntity.ok(response);
    }


}
