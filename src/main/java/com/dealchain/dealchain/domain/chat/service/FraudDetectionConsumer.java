
package com.dealchain.dealchain.domain.chat.service;
import com.dealchain.dealchain.domain.AI.dto.detectDto;
import com.dealchain.dealchain.domain.AI.service.ApiService;
import com.dealchain.dealchain.domain.AI.service.ChatPaser;
import com.dealchain.dealchain.domain.chat.dto.SQSrequestDto;
import com.dealchain.dealchain.domain.chat.entity.ChatRoom;
import com.dealchain.dealchain.domain.chat.repository.ChatRoomRepository;
import com.dealchain.dealchain.domain.contract.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionConsumer {

    private static final String FRAUD_DETECTION_QUEUE = "my-fraud-queue";

    private final ObjectMapper objectMapper;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatPaser chatPaser;
    private final NotificationService notificationService;
    private final ApiService flaskApiService;

    @Value("${MESSAGE_COUNT:10}")
    private int batchSize;

    @Value("${THRESHOLD:0.7}")
    private double threshold;

    // 내부 버퍼: SQSrequestDto들을 쌓아둔다
    private final BlockingQueue<SQSrequestDto> buffer = new LinkedBlockingQueue<>();

    @SqsListener(
            value = FRAUD_DETECTION_QUEUE,
            factory = "batchSqsListenerContainerFactory"
    )
    public void receiveFraudDetectionMessages(@Payload List<Message<String>> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        log.info("SQS에서 {}개의 메시지 수신.", messages.size());

        for (Message<String> msg : messages) {
            String payload = msg.getPayload();
            if (payload == null || payload.trim().isEmpty()) {
                log.warn("빈 페이로드 건너뜀. headers={}", msg.getHeaders());
                continue;
            }
            try {
                SQSrequestDto dto = objectMapper.readValue(payload, SQSrequestDto.class);
                if (dto == null) {
                    log.warn("파싱된 DTO가 null 입니다. payload={}", payload);
                    continue;
                }
                if (isValid(dto)) {
                    buffer.offer(dto);
                } else {
                    log.warn("유효하지 않은 SQS 메시지: {}", dto);
                }
            } catch (Exception e) {
                log.error("SQS 메시지 파싱 실패. payload={}", payload, e);
            }
        }

        // 버퍼에 batchSize 이상 모였으면 하나 또는 여러 배치 처리
        while (buffer.size() >= Math.max(1, batchSize)) {
            processBatchFromBuffer(batchSize);
        }
    }

    private void processBatchFromBuffer(int limit) {
        List<SQSrequestDto> drainList = new ArrayList<>(limit);
        buffer.drainTo(drainList, limit);
        if (drainList.isEmpty()) {
            return;
        }
        log.info("버퍼에서 {}개 꺼내 처리 시작.", drainList.size());
        processDtos(drainList);
    }


    private void processDtos(List<SQSrequestDto> parsedDtos) {
        Map<String, List<SQSrequestDto>> messagesByRoom = parsedDtos.stream()
                .collect(Collectors.groupingBy(SQSrequestDto::getRoomId));

        for (Map.Entry<String, List<SQSrequestDto>> entry : messagesByRoom.entrySet()) {
            String roomId = entry.getKey();
            List<SQSrequestDto> roomMessages = entry.getValue();
            try {
                log.info("AI 사기 탐지 모델 호출 예정. RoomId: {}, MessageCount: {}", roomId, roomMessages.size());
                String chatLog = chatPaser.buildSenderToContentsJsonByRoomId(roomId);

                // AI 사기 탐지 모델 호출
                detectDto response = flaskApiService.sendPostRequest(chatLog);

                // 결과가 유효하고 임계값 초과 시 알림 처리
                if (response != null && response.getFraud_score() != null && response.getFraud_score() >= threshold) {

                    Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findById(roomId);
                    if (chatRoomOpt.isEmpty()) {
                        log.warn("ChatRoom을 찾을 수 없습니다. roomId={}", roomId);
                        continue;
                    }
                    ChatRoom chatRoom = chatRoomOpt.get();
                    Long seller = chatRoom.getSellerId();
                    Long buyer = chatRoom.getBuyerId();
                    if (seller == null || buyer == null) {
                        log.warn("ChatRoom에서 seller/buyer ID를 찾을 수 없습니다. roomId={}, seller={}, buyer={}", roomId, seller, buyer);
                        continue;
                    }

                    Long msgId = response.getMessage_id();
                    if (msgId == null) {
                        log.warn("AI 응답 message_id가 null 입니다. roomId={}", roomId);
                    } else if (Objects.equals(msgId, seller)) {
                        try {
                            notificationService.sendNotification(msgId, seller, roomId,
                                    "위험! 계약서 작성을 권고드립니다.", "WARNING_FRAUD", response.getReason());
                        } catch (Exception e) {
                            log.error("seller 알림 전송 실패. roomId={}, seller={}", roomId, seller, e);
                        }
                    } else if (Objects.equals(msgId, buyer)) {
                        try {
                            notificationService.sendNotification(msgId, buyer, roomId,
                                    "위험! 계약서 작성을 권고드립니다.", "WARNING_FRAUD", response.getReason());
                        } catch (Exception e) {
                            log.error("buyer 알림 전송 실패. roomId={}, buyer={}", roomId, buyer, e);
                        }
                    } else {
                        log.warn("message_id가 seller 또는 buyer와 일치하지 않습니다. RoomId={}, msgId={}, seller={}, buyer={}",
                                roomId, msgId, seller, buyer);
                    }
                } else {
                    log.info("사기점수 미달 또는 응답 없음. RoomId={}, fraud_score={}", roomId,
                            response == null ? null : response.getFraud_score());
                }

            } catch (Exception e) {
                log.error("AI 호출/알림 처리 중 오류. RoomId: {}", roomId, e);
                throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
            }
        }
    }

    private boolean isValid(SQSrequestDto dto) {
        return dto.getRoomId() != null && !dto.getRoomId().trim().isEmpty()
                && dto.getSenderId() != null && dto.getSenderId() > 0
                && dto.getMessage() != null && dto.getMessage().length() < 2048;
    }
}