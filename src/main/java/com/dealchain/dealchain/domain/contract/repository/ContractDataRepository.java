package com.dealchain.dealchain.domain.contract.repository;


import com.dealchain.dealchain.domain.contract.entity.ContractData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractDataRepository extends JpaRepository<ContractData, Long> {

    Optional<ContractData> findByRoomId(String roomId);
    List<ContractData> findBySellerIdOrBuyerId(Long sellerId, Long buyerId);
    Optional<ContractData> findByRoomIdAndSellerIdAndBuyerId(String roomId, Long sellerId, Long buyerId);

}