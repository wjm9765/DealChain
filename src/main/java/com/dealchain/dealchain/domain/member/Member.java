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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "resident_number", nullable = false, unique = true)
    private String residentNumber;
    
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;
    
    public Member(String name, String residentNumber, String phoneNumber) {
        this.name = name;
        this.residentNumber = residentNumber;
        this.phoneNumber = phoneNumber;
    }
}
