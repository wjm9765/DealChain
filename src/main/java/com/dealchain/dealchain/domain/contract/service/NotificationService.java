package com.dealchain.dealchain.domain.contract.service;

import com.dealchain.dealchain.domain.chat.entity.ChatNotification;
import com.dealchain.dealchain.domain.chat.repository.ChatNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation; // Propagation 임포트

import java.util.Map;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatNotificationRepository chatNotificationRepository;

    @Autowired
    public NotificationService(SimpMessagingTemplate messagingTemplate,
                               ChatNotificationRepository chatNotificationRepository) {
        this.messagingTemplate = messagingTemplate;
        this.chatNotificationRepository = chatNotificationRepository;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW, transactionManager = "chatTransactionManager") // 알림은 chat DB 트랜잭션 사용
    public void sendNotification(Long who, Long sender, String roomId, String message, String type, String AIcontent) {

        // AIcontent = AI가 왜 그렇게 판단했는지 근거가 되는 json 형태
        try {
            Map<String, String> notificationPayload = Map.of(
                    "type", type,
                    "message", message,
                    "roomId", roomId
            );
            // 알림 전송
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(who),
                    "/queue/notifications",
                    notificationPayload
            );

            ChatNotification chatNotification = ChatNotification.builder()
                    .memberId(who)
                    .senderId(sender)
                    .roomId(roomId)
                    .type(ChatNotification.NotificationType.valueOf(type))
                    .AIContent(AIcontent)
                    .build();

            chatNotificationRepository.save(chatNotification);

            log.info("(ID: {})에게 {}: {}  알림 전송 완료 (RoomId: {})", who, type, message, roomId);

        } catch (Exception e) {
            log.warn("WebSocket 알림 전송/저장 실패 (비동기): {}", e.getMessage());
        }
    }
}