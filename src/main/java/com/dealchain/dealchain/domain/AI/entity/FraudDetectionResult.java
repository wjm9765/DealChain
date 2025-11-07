package com.dealchain.dealchain.domain.AI.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_detection_result")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudDetectionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fraud_score")
    private Double fraudScore;

    @Column(name = "fraud_type", length = 512)
    private String fraudType;

    @Column(name = "message_id")
    private Long messageId;

    @Lob
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;
}

