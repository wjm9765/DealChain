package com.dealchain.dealchain.domain.chat.controller;

import com.dealchain.dealchain.domain.chat.dto.WebMessageDto;
import com.dealchain.dealchain.domain.chat.service.WebChatService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class WebChatController {
    private static final Logger log = LoggerFactory.getLogger(WebChatController.class);
    private final WebChatService chatService;
    private final SimpMessageSendingOperations messagingTemplate;

    @MessageMapping("/chat/message") // 클라이언트가 메시지를 보낼 경로
    public void message(WebMessageDto message, Principal principal) {
        if (message == null) {
            // 입력 검증: 메시지 없으면 무시
            return;
        }

        try {
            // 실제 로직 처리는 서비스 레이어에 위임 (Principal은 JwtChannelInterceptor로 설정됨)
            chatService.handleChatMessage(message, principal);
        } catch (Exception e) {
            // 간단한 예외 처리: 로그 출력 및 해당 사용자에게 에러 전송
            log.error("Error handling chat message: {}", e.getMessage(), e);
            if (principal != null && principal.getName() != null) {
                // 사용자 전용 큐로 에러 전송 (클라이언트는 /user/queue/errors 구독 가능)
                messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", e.getMessage());
            }
        }
    }
}
