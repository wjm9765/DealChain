package com.dealchain.dealchain.domain.DealTracking.service;

import com.dealchain.dealchain.domain.DealTracking.dto.DealTrackingRequest;
import com.dealchain.dealchain.domain.DealTracking.entity.DealTrackingData;
import com.dealchain.dealchain.domain.DealTracking.repository.DealTrackingRepository;
import com.dealchain.dealchain.domain.chat.entity.ChatRoom;
import com.dealchain.dealchain.domain.chat.repository.ChatRoomRepository;
import com.dealchain.dealchain.domain.security.HashService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

@Service // DealTrackingService로 이름을 바꾸는 것을 추천합니다.
@RequiredArgsConstructor // Lombok으로 final 필드 자동 주입
public class DealTrackingService { // 클래스 이름 수정 제안

    private final DealTrackingRepository dealTrackingRepository;
    private final HashService hashService;
    private final ChatRoomRepository chatRoomRepository;

    // 함수 호출 전 토큰 인증 처리 완료
    @Transactional(transactionManager = "dealTransactionManager") // DB 처리를 위해 트랜잭션 적용
    public void dealTrack(String type, DealTrackingRequest request) {

        // 1. 인증된 토큰에서 사용자 아이디를 가져옴
        String principalName = SecurityContextHolder.getContext().getAuthentication().getName();

        // 토큰 값 재검증
        Long principalId;
        try {
            principalId = Long.valueOf(principalName);
        } catch (NumberFormatException e) {
            // 토큰의 사용자 ID 형식이 유효하지 않을 때 예외 처리
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "인증된 토큰의 사용자 ID 형식이 유효하지 않습니다.");
        }

        // 거래 당사자 권한 검증
        if(!isUserAuthorizedForRoom(principalId, request.getRoomId(), request.getRole())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 거래에 대한 접근 권한이 없습니다.");
        }

        // 서버 기준 시간으로 해시 생성 (무결성 검증용)
        LocalDateTime currentTimestamp = LocalDateTime.now();
        String hash = hashService.generateHashFromStrings(
                request.getRoomId(),
                principalId,
                request.getDeviceInfo(),
                currentTimestamp,
                type
        );

        // 거래 추적 데이터 저장 (서버 검증된 값 사용)
        DealTrackingData log = DealTrackingData.builder()
                 .userId(principalId.toString()) // 서버 검증 ID
                 .roomId(request.getRoomId())     // 클라이언트 값 (참고용)
                 .deviceInfo(request.getDeviceInfo()) // 클라이언트 값 (참고용)
                 .type(type) // 서버 정의 값
                 .timestamp(currentTimestamp) // 서버 기준 시간
                 .hashValue(hash) // 무결성 검증용 해시
                 .build();

         dealTrackingRepository.save(log);


    }

    /**
     * 사용자가 해당 거래의 당사자(SELLER/BUYER)인지 확인
     */
    private boolean isUserAuthorizedForRoom(Long principalId, String roomId, String role) {
        if (principalId == null || roomId == null || role == null) return false;

        Optional<ChatRoom> maybeRoom = chatRoomRepository.findById(roomId);
        if (!maybeRoom.isPresent()) return false;

        ChatRoom room = maybeRoom.get();

        // 역할에 따른 권한 확인
        if ("SELLER".equalsIgnoreCase(role)) {
            return room.getSellerId() != null && room.getSellerId().equals(principalId);
        }
        if ("BUYER".equalsIgnoreCase(role)) {
            return room.getBuyerId() != null && room.getBuyerId().equals(principalId);
        }

        return false;
    }
}
