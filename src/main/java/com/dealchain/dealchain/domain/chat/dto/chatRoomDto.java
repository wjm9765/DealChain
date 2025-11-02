package com.dealchain.dealchain.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class chatRoomDto {
    private String roomId;
    private Long seller;
    private Long buyer;
    private Long proudctId;
}
