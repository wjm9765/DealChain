package com.dealchain.dealchain.domain.contract.repository;

import com.dealchain.dealchain.domain.contract.entity.SignTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SignRepository extends JpaRepository<SignTable, Long> {
    Optional<SignTable> findByRoomIdAndProductId(String roomId, Long productId);//roomId가 고유하므로 이렇게 조회해도 됨
}
