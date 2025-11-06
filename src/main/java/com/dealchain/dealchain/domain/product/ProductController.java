package com.dealchain.dealchain.domain.product;

import com.dealchain.dealchain.domain.product.dto.ProductRegisterRequestDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
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
            
            Product product = productService.registerProduct(
                    requestDto.getProductName(),
                    requestDto.getTitle(),
                    requestDto.getPrice(),
                    requestDto.getDescription(),
                    memberId,
                    productImagePath
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "상품이 등록되었습니다.");
            response.put("productId", product.getId());
            response.put("productName", product.getProductName());
            response.put("productImage", product.getProductImage());
            response.put("title", product.getTitle());
            
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
            productInfo.put("title", product.getTitle());
            productInfo.put("description", product.getDescription() != null ? product.getDescription() : "");
            productInfo.put("memberId", product.getMemberId());
            // 이미지를 Base64로 인코딩하여 반환
            if (product.getProductImage() != null && !product.getProductImage().isEmpty()) {
                String base64Image = loadImageAsBase64(product.getProductImage());
                productInfo.put("productImage", base64Image != null ? base64Image : "");
            } else {
                productInfo.put("productImage", "");
            }
            
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
            
            // description과 memberId를 제외한 상품 정보만 추출
            List<Map<String, Object>> productList = products.stream()
                    .map(product -> {
                        Map<String, Object> productMap = new HashMap<>();
                        productMap.put("id", product.getId());
                        productMap.put("productName", product.getProductName());
                        productMap.put("title", product.getTitle());
                        productMap.put("price", product.getPrice());
                        // 이미지를 Base64로 인코딩하여 반환
                        if (product.getProductImage() != null && !product.getProductImage().isEmpty()) {
                            String base64Image = loadImageAsBase64(product.getProductImage());
                            productMap.put("productImage", base64Image != null ? base64Image : "");
                        } else {
                            productMap.put("productImage", "");
                        }
                        return productMap;
                    })
                    .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("products", productList);
            response.put("count", productList.size());
            
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
            
            // description과 memberId를 제외한 상품 정보만 추출
            List<Map<String, Object>> productList = products.stream()
                    .map(product -> {
                        Map<String, Object> productMap = new HashMap<>();
                        productMap.put("id", product.getId());
                        productMap.put("productName", product.getProductName());
                        productMap.put("title", product.getTitle());
                        productMap.put("price", product.getPrice());
                        // 이미지를 Base64로 인코딩하여 반환
                        if (product.getProductImage() != null && !product.getProductImage().isEmpty()) {
                            String base64Image = loadImageAsBase64(product.getProductImage());
                            productMap.put("productImage", base64Image != null ? base64Image : "");
                        } else {
                            productMap.put("productImage", "");
                        }
                        return productMap;
                    })
                    .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("products", productList);
            response.put("count", productList.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // 상품 이미지 조회 API (이미지 파일 직접 반환)
    @GetMapping("/{productId}/image")
    public ResponseEntity<byte[]> getProductImage(@PathVariable("productId") Long productId) {
        try {
            Product product = productService.findById(productId);
            
            if (product == null || product.getProductImage() == null || product.getProductImage().isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // 이미지 파일 경로
            String imagePath = product.getProductImage();
            Path filePath = Paths.get(imagePath);
            
            // 파일이 존재하는지 확인
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            // 파일 읽기
            byte[] imageBytes = Files.readAllBytes(filePath);
            
            // Content-Type 결정
            String contentType = getContentType(imagePath);
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(imageBytes.length);
            
            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
    
    // 파일 확장자에 따라 Content-Type 결정
    private String getContentType(String filePath) {
        String lowerPath = filePath.toLowerCase();
        if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerPath.endsWith(".png")) {
            return "image/png";
        } else if (lowerPath.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerPath.endsWith(".webp")) {
            return "image/webp";
        } else {
            return "application/octet-stream";
        }
    }
    
    // 이미지 파일을 Base64로 인코딩하여 반환
    private String loadImageAsBase64(String imagePath) {
        try {
            Path filePath = Paths.get(imagePath);
            
            // 파일이 존재하는지 확인
            if (!Files.exists(filePath)) {
                return null;
            }
            
            // 파일 읽기
            byte[] imageBytes = Files.readAllBytes(filePath);
            
            // Base64로 인코딩
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            
            // Content-Type에 따라 data URI 형식으로 반환
            String contentType = getContentType(imagePath);
            return "data:" + contentType + ";base64," + base64Image;
            
        } catch (IOException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
