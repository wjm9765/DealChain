package com.dealchain.dealchain.domain.product;

import com.dealchain.dealchain.domain.product.dto.ProductRegisterRequestDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    // 상품 등록 API (로그인 필요, 이미지 포함)
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> registerProduct(
            @Valid @ModelAttribute ProductRegisterRequestDto requestDto,
            @RequestParam(value = "productImage", required = false) MultipartFile productImage,
            Authentication authentication) {
        try {
            // 인증 확인
            if (authentication == null || !authentication.isAuthenticated()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(response);
            }
            
            // JWT에서 사용자 ID 가져오기
            Long memberId = Long.valueOf(authentication.getName());
            
            String productImagePath = null;
            
            // 상품 이미지가 있는 경우 저장
            if (productImage != null && !productImage.isEmpty()) {
                productImagePath = saveImage(productImage, "products");
            }
            
            Product product = productService.registerProduct(requestDto.getProductName(), requestDto.getPrice(), requestDto.getDescription(), memberId, productImagePath);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "상품이 등록되었습니다.");
            response.put("productId", product.getId());
            response.put("productName", product.getProductName());
            response.put("productImage", product.getProductImage());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // 상품 삭제 API (로그인 필요, 본인 상품만 삭제 가능)
    @DeleteMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> deleteProduct(
            @PathVariable("productId") Long productId, 
            Authentication authentication) {
        try {
            // 인증 확인
            if (authentication == null || !authentication.isAuthenticated()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(response);
            }
            
            // JWT에서 사용자 ID 가져오기
            Long memberId = Long.valueOf(authentication.getName());
            
            productService.deleteProduct(productId, memberId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "상품이 삭제되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // 상품 정보 조회 API
    @GetMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> getProduct(@PathVariable("productId") Long productId) {
        try {
            Product product = productService.findById(productId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            Map<String, Object> productInfo = new HashMap<>();
            productInfo.put("id", product.getId());
            productInfo.put("productName", product.getProductName());
            productInfo.put("price", product.getPrice());
            productInfo.put("description", product.getDescription() != null ? product.getDescription() : "");
            productInfo.put("memberId", product.getMemberId());
            productInfo.put("productImage", product.getProductImage() != null ? product.getProductImage() : "");
            
            response.put("product", productInfo);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // 전체 상품 목록 조회 API
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllProducts() {
        try {
            List<Product> products = productService.findAllProducts();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("products", products);
            response.put("count", products.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // 특정 회원의 상품 목록 조회 API
    @GetMapping("/member/{memberId}")
    public ResponseEntity<Map<String, Object>> getProductsByMember(@PathVariable("memberId") Long memberId) {
        try {
            List<Product> products = productService.findProductsByMemberId(memberId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("products", products);
            response.put("count", products.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // 이미지 저장 헬퍼 메서드
    private String saveImage(MultipartFile image, String folder) throws IOException {
        // 업로드 디렉토리 생성
        String uploadDir = "uploads/" + folder;
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // 고유한 파일명 생성
        String originalFilename = image.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String filename = UUID.randomUUID().toString() + extension;
        
        // 파일 저장
        Path filePath = uploadPath.resolve(filename);
        Files.copy(image.getInputStream(), filePath);
        
        return uploadDir + "/" + filename;
    }
}
