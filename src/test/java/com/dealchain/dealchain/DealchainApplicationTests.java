package com.dealchain.dealchain;

import com.dealchain.dealchain.domain.DealTracking.dto.DealTrackingRequest;
import com.dealchain.dealchain.domain.DealTracking.repository.DealTrackingRepository;
import com.dealchain.dealchain.domain.DealTracking.service.DealTrackingService;
import com.dealchain.dealchain.domain.chat.entity.ChatRoom;
import com.dealchain.dealchain.domain.chat.repository.ChatRoomRepository;
import com.dealchain.dealchain.domain.security.HashService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DealTrackingServiceUnitTest {

    // ... (필드 선언은 동일) ...
    @Mock private DealTrackingRepository dealTrackingRepository;
    @Mock private HashService hashService;
    @Mock private ChatRoomRepository chatRoomRepository;
    @InjectMocks private DealTrackingService dealTrackingService;

    private final String ROOM_ID = "1d5d98ea-5644-4df3-bfc9-050ad11bc66d";
    private final Long SELLER_ID = 14L;
    private final Long BUYER_ID = 11L;
    private final String ACTION_TYPE = "SIGN";

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void callDealTrack_SellerPerformsAction_shouldNotThrowAndSaveLog() {
        // 1. SecurityContextHolder Mocking (ID 14로 로그인)
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn(String.valueOf(SELLER_ID));

        // 2. ChatRoom Mock 설정
        ChatRoom room = ChatRoom.builder()
                .roomId(ROOM_ID)
                .sellerId(SELLER_ID)
                .buyerId(BUYER_ID)
                .build();
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));

        // 3. DTO Mock 설정
        DealTrackingRequest request = mock(DealTrackingRequest.class);
        when(request.getRoomId()).thenReturn(ROOM_ID);
        when(request.getRole()).thenReturn("SELLER");
        when(request.getDeviceInfo()).thenReturn("TestDevice");

        // 4. [수정됨] HashService Mock 설정: 타입별 매처 사용
        // Mockito는 Long을 anyLong() 대신 any(Long.class)로,
        // LocalDateTime은 any(LocalDateTime.class)로 처리해야 안전합니다.
        when(hashService.generateHashFromStrings(
                anyString(),                  // roomId (String)
                any(Long.class),              // principalId (Long) 👈 NPE 해결
                anyString(),                  // deviceInfo (String)
                any(LocalDateTime.class),     // currentTimestamp (LocalDateTime) 👈 NPE 해결
                anyString()                   // type (String)
        )).thenReturn("MOCK_HASH_VALUE");

        // 5. 함수 호출 및 검증
        assertDoesNotThrow(() ->
                dealTrackingService.dealTrack(ACTION_TYPE, request)
        );

        verify(dealTrackingRepository).save(any());
    }
}