package com.dealchain.dealchain.domain.chat.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ChatRoomListResponseDto {
    private List<chatRoomDto> chatRooms;
    private boolean success;
    private String message;
}