// language: java
package com.dealchain.dealchain.domain.chat.service;

import com.dealchain.dealchain.domain.chat.dto.SQSrequestDto;
import com.dealchain.dealchain.domain.chat.entity.ChatRoom;
import com.dealchain.dealchain.domain.chat.repository.ChatRoomRepository;
import com.dealchain.dealchain.domain.contract.service.ContractService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionConsumer {

    private static final String FRAUD_DETECTION_QUEUE = "my-fraud-queue";

    private final ObjectMapper objectMapper;
    private final SqsAsyncClient sqsAsyncClient;
    private final ContractService contractService;
    private final ChatRoomRepository chatRoomRepository;
    // private final AiModelService aiModelService; // AI 호출은 주석으로 유지
    // private final NotificationService notificationService; // 예외 알림을 사용하려면 주입 후 아래 주석 해제

    @SqsListener(
            value = FRAUD_DETECTION_QUEUE,
            factory = "batchSqsListenerContainerFactory"
    )
    public void receiveFraudDetectionMessages(@Payload List<Message<SQSrequestDto>> messages) {
        log.info("SQS에서 {}개의 메시지 배치를 수신했습니다.", messages.size());
        if (messages.isEmpty()) {
            return;
        }

        List<Message<SQSrequestDto>> validMessages = new ArrayList<>();
        for (Message<SQSrequestDto> msg : messages) {
            SQSrequestDto dto = msg.getPayload();
            if (dto == null) {
                log.warn("빈 페이로드 메시지 건너뜀.");
                continue;
            }
            if (isValid(dto)) {
                validMessages.add(msg);
            } else {
                log.warn("유효하지 않은 SQS 메시지: {}", dto);
            }
        }

        if (validMessages.isEmpty()) {
            log.info("유효한 메시지 없음. 처리 종료.");
            return;
        }

        Map<String, List<SQSrequestDto>> messagesByRoom = validMessages.stream()
                .map(Message::getPayload)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(SQSrequestDto::getRoomId));

        // 각 room별 처리. 예외 발생 시 알림 호출(주석) 후 예외 재던짐 -> AUTO 모드면 메시지 삭제 안 됨
        for (Map.Entry<String, List<SQSrequestDto>> entry : messagesByRoom.entrySet()) {
            String roomId = entry.getKey();
            List<SQSrequestDto> roomMessages = entry.getValue();
            try {
                String jsonString = objectMapper.writeValueAsString(roomMessages);


                //테스트용
                System.out.println("5개 문자"+jsonString);
                log.info("AI 사기 탐지 모델 호출 예정. RoomId: {}, MessageCount: {}", roomId, roomMessages.size());
                // aiModelService.callAiModel(roomId, roomMessages); // 실제 호출(주석)
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
                //일단 구매자한테 보냄
                contractService.sendNotification(buyer,seller,roomId,"위험! 계약서 작성을 권고드립니다.","WARNING_FRAUD",null);
            } catch (Exception e) {
                log.error("AI 호출/알림 처리 중 오류. RoomId: {}", roomId, e);
                throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
            }
        }

        //정상으로 끝났다면 자동 삭제
    }

    private boolean isValid(SQSrequestDto dto) {
        return dto.getRoomId() != null && !dto.getRoomId().trim().isEmpty()
                && dto.getSenderId() != null && dto.getSenderId() > 0
                && dto.getMessage() != null && dto.getMessage().length() < 2048;
    }
}
