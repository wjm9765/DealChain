package com.dealchain.dealchain.domain.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WebMessageDto {

    public enum MessageType {
        ENTER, TALK, LEAVE//입장 대화 퇴장
    }

    private MessageType type;
    private String roomId;
    private Long senderId;
    private String message;

    //선택사항
    private Long timestamp;
}
