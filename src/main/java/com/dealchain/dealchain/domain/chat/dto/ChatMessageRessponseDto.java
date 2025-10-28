package com.dealchain.dealchain.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ChatMessageRessponseDto {
    private String roomID;
    private List<ChatMessageDto> messages;
    private boolean isexist;
}
