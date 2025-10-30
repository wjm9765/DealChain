package com.dealchain.dealchain.domain.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessageRequestDto {
    //rivate Long roomId;     // 메시지가 속할 채팅방 ID
    private Long seller; //대화 방에서 판매자
    private Long buyer;  //대화 방에서 구매자
    private Long user;//누가 요청했는지
}