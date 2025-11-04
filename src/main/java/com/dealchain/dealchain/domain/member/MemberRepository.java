package com.dealchain.dealchain.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    
    // id(String)로 회원 찾기 (로그인용)
    @Query("SELECT m FROM Member m WHERE m.id = :id")
    Optional<Member> findByIdString(@Param("id") String id);
    
    // id(String) 중복 체크용
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Member m WHERE m.id = :id")
    boolean existsByIdString(@Param("id") String id);
}
