package com.dealchain.dealchain.domain.contract;

import com.dealchain.dealchain.domain.contract.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
}


