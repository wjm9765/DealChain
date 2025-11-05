package com.dealchain.dealchain.domain.contract;

import com.dealchain.dealchain.domain.contract.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    List<Contract> findBySellerIdOrBuyerId(Long sellerId, Long buyerId);
    Optional<Contract> findByRoomId(String roomId);
}


