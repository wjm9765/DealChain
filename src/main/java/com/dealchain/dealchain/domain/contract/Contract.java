package com.dealchain.dealchain.domain.contract;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "contracts")
@Getter
@Setter
@NoArgsConstructor
public class Contract {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "file_path", nullable = false, columnDefinition = "TEXT")
    private String filePath;
    
    @Column(name = "seller_id")
    private Long sellerId;
    
    @Column(name = "buyer_id")
    private Long buyerId;
    
    @Column(name = "room_id")
    private Long roomId;
    
    @Column(name = "encrypted_hash", columnDefinition = "TEXT")
    private String encryptedHash;
    
    public Contract(String filePath) {
        this.filePath = filePath;
    }
    
    public Contract(String filePath, Long sellerId, Long buyerId, Long roomId) {
        this.filePath = filePath;
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.roomId = roomId;
    }
    
    public Contract(String filePath, Long sellerId, Long buyerId, Long roomId, String encryptedHash) {
        this.filePath = filePath;
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.roomId = roomId;
        this.encryptedHash = encryptedHash;
    }
}


