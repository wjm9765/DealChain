package com.dealchain.dealchain.domain.DealTracking.repository;


import com.dealchain.dealchain.domain.DealTracking.entity.DealTrackingData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DealTrackingRepository extends JpaRepository<DealTrackingData, Long> {
    // 기본 save(entity)로 insert 가능, deleteById(id) 등으로 삭제 가능
}
