package com.dealchain.dealchain.domain.contract;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    @Autowired
    private ContractService contractService;

    /**
     * PDF 파일을 업로드하여 S3에 저장하고 경로를 RDS에 저장합니다.
     *
     * POST /api/contracts/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadContract(
            @RequestParam("pdf") MultipartFile pdfFile,
            @RequestParam(value = "sellerId", required = false) Long sellerId,
            @RequestParam(value = "buyerId", required = false) Long buyerId,
            @RequestParam(value = "roomId", required = false) Long roomId) {
        try {
            if (pdfFile == null || pdfFile.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "PDF 파일이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }

            Contract contract = contractService.uploadAndSaveContract(pdfFile, sellerId, buyerId, roomId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "계약서가 업로드되었습니다.");
            response.put("contractId", contract.getId());
            response.put("filePath", contract.getFilePath());
            response.put("sellerId", contract.getSellerId());
            response.put("buyerId", contract.getBuyerId());
            response.put("roomId", contract.getRoomId());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "계약서 업로드 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * ID로 계약서를 조회하고 S3에서 PDF를 다운로드하여 반환합니다.
     *
     * GET /api/contracts/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getContractPdf(@PathVariable("id") Long id) {
        try {
            ContractService.ContractPdfResult result = contractService.getContractPdf(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentLength(result.getPdfBytes().length);
            headers.setContentDispositionFormData("attachment", "contract_" + id + ".pdf");

            return new ResponseEntity<>(result.getPdfBytes(), headers, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 임시로 아무 PDF나 반환합니다.
     * (현재는 기존에 업로드된 PDF 중 첫 번째를 반환하거나, 없으면 에러 반환)
     *
     * POST /api/contracts/create
     */
    @PostMapping("/create")
    public ResponseEntity<byte[]> getTempPdf() {
        try {
            // 임시로 기존에 업로드된 PDF 중 첫 번째를 반환
            // 실제로는 샘플 PDF를 생성하거나 특정 PDF를 반환해야 합니다.
            ContractService.ContractPdfResult result = contractService.getFirstContractPdf();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentLength(result.getPdfBytes().length);
            headers.setContentDispositionFormData("attachment", "temp_contract.pdf");

            return new ResponseEntity<>(result.getPdfBytes(), headers, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            // 계약서가 없을 경우 임시 PDF 바이트 배열 반환 (빈 PDF)
            // 실제로는 샘플 PDF 생성 로직이 필요합니다.
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            return ResponseEntity.ok().headers(headers).body(new byte[0]);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * ID로 계약서의 PDF를 수정합니다. (S3의 같은 경로로 교체)
     *
     * PUT /api/contracts/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateContract(
            @PathVariable("id") Long id,
            @RequestParam("pdf") MultipartFile pdfFile) {
        try {
            if (pdfFile == null || pdfFile.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "PDF 파일이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }

            Contract contract = contractService.updateContractPdf(id, pdfFile);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "계약서가 수정되었습니다.");
            response.put("contractId", contract.getId());
            response.put("filePath", contract.getFilePath());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "계약서 수정 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * ID로 계약서를 삭제합니다. (DB와 S3에서 모두 삭제)
     *
     * DELETE /api/contracts/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteContract(@PathVariable("id") Long id) {
        try {
            contractService.deleteContract(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "계약서가 삭제되었습니다.");
            response.put("contractId", id);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "계약서 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

