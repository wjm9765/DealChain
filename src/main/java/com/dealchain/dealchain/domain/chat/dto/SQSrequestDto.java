package com.dealchain.dealchain.domain.chat.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SQSrequestDto {
    private String message;
    private Long senderId;
    private Long receiverId;
    private String roomId;
    private Long messageId;
}
