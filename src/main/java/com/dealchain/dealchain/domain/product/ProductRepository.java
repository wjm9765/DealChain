package com.dealchain.dealchain.domain.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // 특정 회원이 등록한 상품 목록 조회
    List<Product> findByMemberId(Long memberId);
    
    // 특정 회원이 등록한 상품 개수 조회
    long countByMemberId(Long memberId);
}
