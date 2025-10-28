package com.dealchain.dealchain.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class ChatMessageDto {
    private Long messageId;
    private String senderId;
    private String content;
    private LocalDateTime timestamp;
}
