package com.dealchain.dealchain.domain.contract;

import com.dealchain.dealchain.domain.AI.dto.ContractDefaultReqeustDto;
import com.dealchain.dealchain.domain.AI.service.AICreateContract;
import com.dealchain.dealchain.domain.AI.service.ChatPaser;
import com.dealchain.dealchain.domain.DealTracking.service.DealTrackingService;
import com.dealchain.dealchain.domain.chat.repository.ChatRoomRepository;
import com.dealchain.dealchain.domain.contract.dto.ContractCreateRequestDto;
import com.dealchain.dealchain.domain.contract.dto.ContractResponseDto;
import com.dealchain.dealchain.domain.product.Product;
import com.dealchain.dealchain.domain.product.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {
    private final ContractService contractService;
    private final ChatPaser chatPaser;
    private final AICreateContract AICreateContract;
    private final ProductService productService;
    private final ChatRoomRepository chatRoomRepository;
    @Autowired // (Spring 4.3+ 부터 생성자가 1개면 @Autowired 생략 가능)
    public ContractController(ContractService contractService,
                              AICreateContract aiCreateContract,
                              ChatPaser chatPaser,
                              ProductService productService,
                              ChatRoomRepository chatRoomRepository
    ) {
        this.contractService = contractService;
        this.AICreateContract = aiCreateContract;
        this.chatPaser = chatPaser;
        this.productService = productService;
        this.chatRoomRepository = chatRoomRepository;
    }
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
            @RequestParam(value = "roomId", required = false) String roomId) {
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
            response.put("encryptedHash", contract.getEncryptedHash());

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
     * 계약서 생성 로직
     *
     * POST /api/contracts/create
     * @param requestDto (ContractCreateRequestDto: roomId 포함)
     * @return ContractResponseDto (isSuccess, data: AI가 생성한 JSON 문자열)
     **/
    @PostMapping("/create")
    public ResponseEntity<ContractResponseDto> createContractFromChat(@RequestBody ContractCreateRequestDto requestDto) {


        try {
            String roomId = requestDto.getRoomId();


            if (roomId == null || roomId.isEmpty()) {
                ContractResponseDto errorResponse = ContractResponseDto.builder()
                        .isSuccess(false)
                        .data(null)
                        .build();
                return ResponseEntity.badRequest().body(errorResponse);
            }

            //0. 거래 추적 테이블 작성
            contractService.recordDealTrackingForCreate(
                    "CREATE", // 추적 유형
                    requestDto.getRoomId(),
                    requestDto.getSellerId(),
                    requestDto.getBuyerId(),
                    requestDto.getDeviceInfo()
            );


            // 1. roomId로 대화 내역(String) 조회
            String chatLog = chatPaser.buildSenderToContentsJsonByRoomId(roomId);

            //대화 내역 뿐만 아니라 상품에 대한 정보도 보내야됨 description
            Optional<Long> productIdOpt = chatRoomRepository.findProductIdByRoomId(roomId);
            Long productId = productIdOpt.orElseThrow(
                    () -> new IllegalArgumentException("해당 roomId에 대한 productId가 없습니다. roomId=" + roomId)
            );
            Product product = productService.findById(productId);


            ContractDefaultReqeustDto default_request = ContractDefaultReqeustDto.builder()
                    .sellerId(requestDto.getSellerId())
                    .buyerId(requestDto.getBuyerId())
                    .product(product)
                    .build();

            // 2. 대화 내역을 Bedrock AI에게 전송
            String aiContractJson = AICreateContract.invokeClaude(chatLog,default_request);


            // 3. 성공 응답(DTO) 생성
            ContractResponseDto successResponse = ContractResponseDto.builder()
                    .isSuccess(true)
                    .data(aiContractJson) // AI가 생성한 JSON 문자열
                    .build();


            return ResponseEntity.ok(successResponse);

        } catch (Exception e) {

            // 오류 발생 시, 서버 로그에만 상세 내용을 기록하기
            System.err.println("Failed to create contract from chat for roomId: {}" + e.getMessage());
            ContractResponseDto errorResponse = ContractResponseDto.builder()
                    .isSuccess(false)
                    .data(null)
                    .build();

            // 500 Internal Server Error 반환
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
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

