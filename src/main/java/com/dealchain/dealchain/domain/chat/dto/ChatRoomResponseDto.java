package com.dealchain.dealchain.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor//모든 필드를 인자로 받는 생성자를 자동 생성
public class ChatRoomResponseDto {
    private String roomId;
    //private String roomName;
    private boolean ismake;
    private String exception;
}
