package com.dealchain.dealchain.domain.DealTracking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "deal_tracking")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class DealTrackingData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false, updatable = false)
    private String roomId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private String userId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(name = "device_info", updatable = false)
    private String deviceInfo;

    // 한 번 저장된 해시값은 JPA로 업데이트되지 않도록 updatable = false, setter 제거
    @Column(name = "hash_value", updatable = false)
    private String hashValue;

    @PrePersist
    private void onCreate() {
        this.timestamp = LocalDateTime.now();
        //this.hashValue = generateHashValue();
    }
}
