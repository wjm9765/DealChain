package com.dealchain.dealchain.domain.product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    // 상품 등록 API
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> registerProduct(@RequestBody Map<String, Object> request) {
        try {
            String productName = (String) request.get("productName");
            Long price = Long.valueOf(request.get("price").toString());
            String description = (String) request.get("description");
            Long memberId = Long.valueOf(request.get("memberId").toString());
            
            Product product = productService.registerProduct(productName, price, description, memberId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "상품이 등록되었습니다.");
            response.put("productId", product.getId());
            response.put("productName", product.getProductName());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // 상품 삭제 API
    @DeleteMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> deleteProduct(
            @PathVariable Long productId, 
            @RequestParam Long memberId) {
        try {
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
    public ResponseEntity<Map<String, Object>> getProduct(@PathVariable Long productId) {
        try {
            Product product = productService.findById(productId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("product", Map.of(
                "id", product.getId(),
                "productName", product.getProductName(),
                "price", product.getPrice(),
                "description", product.getDescription(),
                "memberId", product.getMemberId()
            ));
            
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
    public ResponseEntity<Map<String, Object>> getProductsByMember(@PathVariable Long memberId) {
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
}
