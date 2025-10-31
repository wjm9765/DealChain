package com.dealchain.dealchain.domain.chat.controller;

import com.dealchain.dealchain.domain.chat.dto.*;
import com.dealchain.dealchain.domain.chat.service.APIChatService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

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

    @PostMapping("/getchatrooms")
    public ResponseEntity<ChatRoomListResponseDto> getChatRooms(
            @RequestBody ChatRoomUserRequestDto request,
            Authentication authentication
    ) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰이 존재하지 않습니다.");
        }

        Long principalId;
        try {
            principalId = Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰의 사용자 ID 형식이 유효하지 않습니다: " + authentication.getName());
        }

        Long userId = request.getUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId가 없습니다.");
        }

        if (!principalId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "토큰의 사용자와 요청의 userId가 일치하지 않습니다: " + authentication.getName());
        }

        ChatRoomListResponseDto response = chatService.getChatRooms(userId);
        return ResponseEntity.ok(response);
    }

}
