
package com.dealchain.dealchain.domain.chat.service;

import com.dealchain.dealchain.domain.AI.service.ChatPaser;
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
    private final ChatPaser chatPaser;

    @SqsListener(
            value = FRAUD_DETECTION_QUEUE,
            factory = "batchSqsListenerContainerFactory"
    )
    public void receiveFraudDetectionMessages(@Payload List<Message<String>> messages) {
        log.info("SQS에서 {}개의 메시지 배치를 수신했습니다.", messages.size());
        if (messages.isEmpty()) {
            return;
        }

        List<SQSrequestDto> parsedDtos = new ArrayList<>();
        for (Message<String> msg : messages) {
            String payload = msg.getPayload();
            if (payload == null || payload.trim().isEmpty()) {
                log.warn("빈 페이로드 메시지 건너뜀. headers={}", msg.getHeaders());
                continue;
            }
            try {
                SQSrequestDto dto = objectMapper.readValue(payload, SQSrequestDto.class);
                if (dto == null) {
                    log.warn("파싱된 DTO가 null 입니다. payload={}", payload);
                    continue;
                }
                if (isValid(dto)) {
                    parsedDtos.add(dto);
                } else {
                    log.warn("유효하지 않은 SQS 메시지: {}", dto);
                }
            } catch (Exception e) {
                log.error("SQS 메시지 파싱 실패. payload={}", payload, e);

            }
        }

        if (parsedDtos.isEmpty()) {
            log.info("유효한 메시지 없음. 처리 종료.");
            return;
        }

        Map<String, List<SQSrequestDto>> messagesByRoom = parsedDtos.stream()
                .collect(Collectors.groupingBy(SQSrequestDto::getRoomId));

        for (Map.Entry<String, List<SQSrequestDto>> entry : messagesByRoom.entrySet()) {
            String roomId = entry.getKey();
            List<SQSrequestDto> roomMessages = entry.getValue();
            try {
                String chatLog = chatPaser.buildSenderToContentsJsonByRoomId(roomId);


                log.info("AI 사기 탐지 모델 호출 예정. RoomId: {}, MessageCount: {}", roomId, roomMessages.size());

                System.out.println("테스트!!!!!!!!: " + chatLog);
                //AI 탐지

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
                contractService.sendNotification(buyer, seller, roomId, "위험! 계약서 작성을 권고드립니다.", "WARNING_FRAUD", null);
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
