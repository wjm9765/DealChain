// java
package com.dealchain.dealchain.domain.AI.service;

import com.dealchain.dealchain.domain.chat.entity.ChatMessage;
import com.dealchain.dealchain.domain.chat.repository.ChatMessageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatPaser {

    private final ChatMessageRepository chatMessageRepository;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * roomId에 있는 채팅들을 시간순으로 조회해
     * [{"senderId":"내용"}, {"senderId":"내용"}, ...] 형태의 JSON 문자열로 반환.
     */
    @Transactional(readOnly = true)
    public String buildSenderToContentsJsonByRoomId(String roomId) { //거래 계약서 생성, 거래 대화 맥락 분석에 사용
        if (roomId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roomId가 필요합니다.");
        }

        List<ChatMessage> messages = chatMessageRepository.findByChatRoom_RoomIdOrderByTimestampAsc(roomId);

        List<Map<String, String>> ordered = new ArrayList<>();
        for (ChatMessage msg : messages) {
            String sender = msg.getSenderId() == null ? "null" : String.valueOf(msg.getSenderId());
            Map<String, String> single = new LinkedHashMap<>();
            single.put(sender, msg.getContent());
            ordered.add(single);
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(ordered);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "JSON 변환 실패", e);
        }
    }
}
