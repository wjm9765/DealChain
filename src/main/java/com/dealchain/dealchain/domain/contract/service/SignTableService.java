package com.dealchain.dealchain.domain.contract.service;

import com.dealchain.dealchain.domain.chat.repository.ChatRoomRepository;
import com.dealchain.dealchain.domain.contract.entity.SignTable;
import com.dealchain.dealchain.domain.contract.repository.SignRepository;
import com.dealchain.dealchain.domain.product.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
public class SignTableService {
    private static final Logger log = LoggerFactory.getLogger(SignTableService.class);


    private final SignRepository signRepository;
    private final ChatRoomRepository chatRoomRepository;

    @Autowired
    public SignTableService(SignRepository signRepository,
                            ChatRoomRepository chatRoomRepository) {
        this.signRepository = signRepository;
        this.chatRoomRepository = chatRoomRepository;
    }


    /**
     * [헬퍼] RoomID로 서명 상태 조회
     */
    public SignTable.SignStatus getSignStatusForRoom(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            log.warn("roomId가 없어 서명 상태를 조회할 수 없습니다.");
            return null;
        }

        Long productId = chatRoomRepository.findProductIdByRoomId(roomId)
                .orElse(null);

        if (productId == null) {
            log.warn("RoomId: {}의 productId가 없어 서명 상태를 조회할 수 없습니다.", roomId);
            return null;
        }

        return signRepository.findByRoomIdAndProductId(roomId, productId)
                .map(SignTable::getStatus)
                .orElse(null);
    }


    @Transactional(transactionManager = "contractTransactionManager") // 트랜잭션 보장
    public SignTable createInitialSignIfNotExists(String roomId, Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product가 필요합니다.");
        }

        Long productId = product.getId();

        // 중복 방지
        Optional<SignTable> existing = signRepository.findByRoomIdAndProductId(roomId, productId);
        if (existing.isPresent()) {
            return existing.get(); // 이미 있으면 기존 객체 반환
        }

        SignTable signTable = SignTable.builder()
                .roomId(roomId)
                .productId(productId)
                .build();

        return signRepository.save(signTable); // 저장 후 영속화된 엔티티 반환
    }


}