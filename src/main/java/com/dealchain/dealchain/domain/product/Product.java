package com.dealchain.dealchain.domain.product;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "product_name", nullable = false)
    private String productName;
    
    @Column(name = "price", nullable = false)
    private Long price;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "member_id", nullable = false)
    private Long memberId;
    
    public Product(String productName, Long price, String description, Long memberId) {
        this.productName = productName;
        this.price = price;
        this.description = description;
        this.memberId = memberId;
    }
}
