package com.dealchain.dealchain.domain.contract.controller;

import com.dealchain.dealchain.domain.contract.entity.Contract;
import com.dealchain.dealchain.domain.contract.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    @Autowired
    private ContractService contractService;

    /**
     * PDF 업로드 및 저장
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> uploadContract(
            @RequestParam("file") MultipartFile file) {
        try {
            // 파일 유효성 검사
            if (file == null || file.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "파일이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }

            // PDF 파일인지 확인
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "PDF 파일만 업로드 가능합니다.");
                return ResponseEntity.badRequest().body(response);
            }

            // 파일 저장
            String filePath = savePdf(file);

            // Contract 저장
            Contract contract = contractService.saveContract(filePath);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "계약서가 저장되었습니다.");
            response.put("contractId", contract.getId().toString());
            response.put("filePath", contract.getFilePath());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IOException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "파일 저장 중 오류가 발생했습니다.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * ID로 계약서 조회 (파일 경로 반환)
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getContract(@PathVariable("id") UUID id) {
        try {
            Contract contract = contractService.findById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            Map<String, Object> contractInfo = new HashMap<>();
            contractInfo.put("id", contract.getId().toString());
            contractInfo.put("filePath", contract.getFilePath());
            response.put("contract", contractInfo);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * ID로 계약서 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteContract(@PathVariable("id") UUID id) {
        try {
            contractService.deleteContract(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "계약서가 삭제되었습니다.");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * PDF 파일 저장 헬퍼 메서드
     */
    private String savePdf(MultipartFile file) throws IOException {
        // 업로드 디렉토리 생성
        String uploadDir = "uploads/contracts";
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 고유한 파일명 생성
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".pdf";
        String filename = UUID.randomUUID().toString() + extension;

        // 파일 저장
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        return uploadDir + "/" + filename;
    }
}

