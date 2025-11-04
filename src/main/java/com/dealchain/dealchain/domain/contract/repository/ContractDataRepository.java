package com.dealchain.dealchain.domain.contract.repository;


import com.dealchain.dealchain.domain.contract.entity.ContractData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContractDataRepository extends JpaRepository<ContractData, Long> {

    Optional<ContractData> findByRoomIdAndSellerIdAndBuyerId(String roomId, Long sellerId, Long buyerId);

}