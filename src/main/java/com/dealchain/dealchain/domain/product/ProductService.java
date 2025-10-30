package com.dealchain.dealchain.domain.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
// 🚨 수정된 부분: 기본 트랜잭션 매니저를 "productTransactionManager"로 지정
@Transactional(transactionManager = "productTransactionManager")
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // 상품 등록 (클래스 레벨의 트랜잭션 매니저 상속)
    public Product registerProduct(String productName, Long price, String description, Long memberId) {
        Product product = new Product(productName, price, description, memberId);
        return productRepository.save(product);
    }

    // 상품 등록 (이미지 포함) (클래스 레벨의 트랜잭션 매니저 상속)
    public Product registerProduct(String productName, Long price, String description, Long memberId, String productImage) {
        Product product = new Product(productName, price, description, memberId, productImage);
        return productRepository.save(product);
    }

    // 상품 삭제 (등록자만 삭제 가능) (클래스 레벨의 트랜잭션 매니저 상속)
    public void deleteProduct(Long productId, Long memberId) {
        Optional<Product> productOpt = productRepository.findById(productId);

        if (productOpt.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 상품입니다.");
        }

        Product product = productOpt.get();
        // 🚨 보안 주의: 여기서 memberId는 현재 로그인한 사용자의 ID와 비교되어야 함.
        // 현재 로직은 인가(Authorization)를 Service 레이어에서 처리하고 있습니다.
        if (!product.getMemberId().equals(memberId)) {
            // 부적절한 인가(Improper Authorization) 방지를 위해 정확한 권한 확인이 필요합니다.
            throw new IllegalArgumentException("본인이 등록한 상품만 삭제할 수 있습니다.");
        }

        productRepository.delete(product);
    }

    // 상품 정보 조회
    // 🚨 수정된 부분: readOnly와 함께 트랜잭션 매니저를 명시합니다.
    @Transactional(readOnly = true, transactionManager = "productTransactionManager")
    public Product findById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));
    }

    // 전체 상품 목록 조회
    // 🚨 수정된 부분: readOnly와 함께 트랜잭션 매니저를 명시합니다.
    @Transactional(readOnly = true, transactionManager = "productTransactionManager")
    public List<Product> findAllProducts() {
        return productRepository.findAll();
    }

    // 특정 회원이 등록한 상품 목록 조회
    // 🚨 수정된 부분: readOnly와 함께 트랜잭션 매니저를 명시합니다.
    @Transactional(readOnly = true, transactionManager = "productTransactionManager")
    public List<Product> findProductsByMemberId(Long memberId) {
        return productRepository.findByMemberId(memberId);
    }
}