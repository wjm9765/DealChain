package com.dealchain.dealchain.domain.member;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "members")
@Getter
@Setter
@NoArgsConstructor
public class Member {
    
    @Id
    @Column(name = "member_id")
    private Long memberId;
    
    @Column(nullable = false, unique = true)
    private String id;
    
    @Column(nullable = false)
    private String password;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "ci", unique = true)
    private String ci;
    
    @Column(name = "signature_image", columnDefinition = "TEXT")
    private String signatureImage;
    
    public Member(String id, String password, String signatureImage) {
        this.id = id;
        this.password = password;
        this.signatureImage = signatureImage;
    }
    
    public Member(String id, String password, String name, String ci, String signatureImage) {
        this.id = id;
        this.password = password;
        this.name = name;
        this.ci = ci;
        this.signatureImage = signatureImage;
    }
}
