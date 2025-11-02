package com.dealchain.dealchain.domain.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRoomRequestDto {
    //private String roomID;
    private Long seller;
    private Long buyer;
    private Long productId;
}
