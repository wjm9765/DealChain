package com.dealchain.dealchain.domain.product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    // 상품 등록
    public Product registerProduct(String productName, Long price, String description, Long memberId) {
        Product product = new Product(productName, price, description, memberId);
        return productRepository.save(product);
    }
    
    // 상품 등록 (이미지 포함)
    public Product registerProduct(String productName, Long price, String description, Long memberId, String productImage) {
        Product product = new Product(productName, price, description, memberId, productImage);
        return productRepository.save(product);
    }
    
    // 상품 삭제 (등록자만 삭제 가능)
    public void deleteProduct(Long productId, Long memberId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        
        if (productOpt.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 상품입니다.");
        }
        
        Product product = productOpt.get();
        if (!product.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("본인이 등록한 상품만 삭제할 수 있습니다.");
        }
        
        productRepository.delete(product);
    }
    
    // 상품 정보 조회
    @Transactional(readOnly = true)
    public Product findById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));
    }
    
    // 전체 상품 목록 조회
    @Transactional(readOnly = true)
    public List<Product> findAllProducts() {
        return productRepository.findAll();
    }
    
    // 특정 회원이 등록한 상품 목록 조회
    @Transactional(readOnly = true)
    public List<Product> findProductsByMemberId(Long memberId) {
        return productRepository.findByMemberId(memberId);
    }
}
