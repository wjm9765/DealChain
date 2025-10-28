package com.dealchain.dealchain.domain.chat.controller;

import com.dealchain.dealchain.domain.chat.dto.WebMessageDto;
import com.dealchain.dealchain.domain.chat.service.WebChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WebChatController {
    private final WebChatService chatService;
    @MessageMapping("/chat/message") // 클라이언트가 메시지를 보낼 경로
    public void message(WebMessageDto message) {
        //System.out.println("api연결 성공");
        try {
            // 실제 로직 처리는 서비스 레이어에 위임
            chatService.handleChatMessage(message);
        } catch (Exception e) {
            // 웹소켓 메시지 처리 중 발생하는 예외는 @MessageExceptionHandler 등으로
            // 별도 처리하는 것이 좋으나, 여기서는 간단히 로그만 남깁니다.
            System.out.println("Error handling chat message: " + e.getMessage());
            //log.error("Error handling chat message: {}", e.getMessage(), e);
            // 필요하다면 에러 메시지를 특정 사용자에게 보내는 로직 추가 가능
            // messagingTemplate.convertAndSendToUser(message.getSenderId(), "/queue/errors", "Error processing message");
        }
    }
}
