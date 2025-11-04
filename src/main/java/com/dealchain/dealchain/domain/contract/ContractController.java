package com.dealchain.dealchain.domain.contract;

import com.dealchain.dealchain.domain.AI.dto.ContractDefaultReqeustDto;
import com.dealchain.dealchain.domain.AI.service.AICreateContract;
import com.dealchain.dealchain.domain.AI.service.ChatPaser;
import com.dealchain.dealchain.domain.chat.entity.ChatRoom;
import com.dealchain.dealchain.domain.chat.repository.ChatRoomRepository;
import com.dealchain.dealchain.domain.contract.dto.ContractCreateRequestDto;
import com.dealchain.dealchain.domain.contract.dto.ContractResponseDto;
import com.dealchain.dealchain.domain.contract.dto.SignRequestDto;
import com.dealchain.dealchain.domain.contract.dto.SignResponseDto;
import com.dealchain.dealchain.domain.contract.entity.Contract;
import com.dealchain.dealchain.domain.contract.entity.SignTable;
import com.dealchain.dealchain.domain.contract.service.ContractService;
import com.dealchain.dealchain.domain.contract.service.JsonToPdfService;
import com.dealchain.dealchain.domain.member.Member;
import com.dealchain.dealchain.domain.member.MemberRepository;
import com.dealchain.dealchain.domain.product.Product;
import com.dealchain.dealchain.domain.product.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {
    private final ContractService contractService;
    private final ChatPaser chatPaser;
    private final AICreateContract AICreateContract;
    private final ProductService productService;
    private final ChatRoomRepository chatRoomRepository;
    private final JsonToPdfService jsonToPdfService;
    private final MemberRepository memberRepository;
    private static final Logger log = LoggerFactory.getLogger(ContractController.class);

    @Autowired
    public ContractController(ContractService contractService,
                              AICreateContract aiCreateContract,
                              ChatPaser chatPaser,
                              ProductService productService,
                              ChatRoomRepository chatRoomRepository,
                              JsonToPdfService jsonToPdfService,
                              MemberRepository memberRepository
    ) {
        this.contractService = contractService;
        this.AICreateContract = aiCreateContract;
        this.chatPaser = chatPaser;
        this.productService = productService;
        this.chatRoomRepository = chatRoomRepository;
        this.jsonToPdfService = jsonToPdfService;
        this.memberRepository = memberRepository;
    }
    @PostMapping("/sign")
    public ResponseEntity<SignResponseDto> signContract(
            @Valid @RequestBody SignRequestDto requestDto) {

        try {

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                SignResponseDto resp = SignResponseDto.builder()
                        .isSuccess(false)
                        .data("Unauthorized: 인증 정보가 없습니다.")
                        .build();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
            }
            Long userId;
            try {
                userId = Long.parseLong(auth.getName());
            } catch (NumberFormatException ex) {
                SignResponseDto resp = SignResponseDto.builder()
                        .isSuccess(false)
                        .data("Unauthorized: 사용자 ID 파싱 실패.")
                        .build();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
            }

            String roomId = requestDto.getRoomId();
            if (roomId == null || roomId.isEmpty()) {
                SignResponseDto resp = SignResponseDto.builder()
                        .isSuccess(false)
                        .data("BadRequest: roomId가 필요합니다.")
                        .build();
                return ResponseEntity.badRequest().body(resp);
            }
            Optional<com.dealchain.dealchain.domain.chat.entity.ChatRoom> roomOpt = chatRoomRepository.findById(roomId);
            if (roomOpt.isEmpty()) {
                SignResponseDto resp = SignResponseDto.builder()
                        .isSuccess(false)
                        .data("NotFound: 해당 roomId가 존재하지 않습니다.")
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
            }

            com.dealchain.dealchain.domain.chat.entity.ChatRoom room = roomOpt.get();

            // 거래 당사자 확인 (SELLER/BUYER)
            Long sellerId = room.getSellerId();
            Long buyerId = room.getBuyerId();
            String role;
            if (sellerId != null && sellerId.equals(userId)) {
                role = "SELLER";
            } else if (buyerId != null && buyerId.equals(userId)) {
                role = "BUYER";
            } else {
                SignResponseDto resp = SignResponseDto.builder()
                        .isSuccess(false)
                        .data("Forbidden: 사용자가 거래 당사자가 아닙니다.")
                        .bothSign(false)
                        .build();
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(resp);
            }


            // 위에서 검증한 데이터로 서비스 레이어 호출
            SignResponseDto response = contractService.signContract(requestDto.getRoomId(),
                    requestDto.getProductId(),
                    userId,
                    role,
                    requestDto.getDeviceInfo());

            // 3. Service의 비즈니스 로직 결과에 따라 응답

            // 양측 서명 완료 시 PDF 생성
            if(response.isBothSign()) {
                Member seller = memberRepository.findById(sellerId)
                        .orElseThrow(() -> new IllegalArgumentException("PDF 생성 실패: 판매자(ID:" + sellerId + ")를 찾을 수 없습니다."));

                Member buyer = memberRepository.findById(buyerId)
                        .orElseThrow(() -> new IllegalArgumentException("PDF 생성 실패: 구매자(ID:" + buyerId + ")를 찾을 수 없습니다."));

                String sellerSignKey = seller.getSignatureImage(); // S3 서명 이미지 키
                String buyerSignKey = buyer.getSignatureImage();  // S3 서명 이미지 키
                String aiJson = requestDto.getContract(); // AI 생성 계약서 JSON

                // JSON과 서명 이미지로 PDF 생성
                byte[] pdfBytes = jsonToPdfService.createPdf(
                        aiJson,
                        sellerSignKey,
                        buyerSignKey
                );
            }

            if (response.isSuccess()) {
                return ResponseEntity.ok(response); // 서명 성공
            } else {
                return ResponseEntity.badRequest().body(response); // 서명 실패 (400)
            }

        } catch (IllegalArgumentException e) { // Service가 "계약서 없음" (404)
            log.warn("Sign failed: Resource not found. (roomId: {}) - {}", requestDto.getRoomId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(SignResponseDto.builder().isSuccess(false).data(e.getMessage()).build());

        } catch (SecurityException e) { // Service가 "권한 없음" (403)
            log.warn("Sign failed: Authorization failed. (roomId: {}) - {}", requestDto.getRoomId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(SignResponseDto.builder().isSuccess(false).data(e.getMessage()).build());

        } catch (Exception e) { // 그 외 모든 서버 오류 (500)
            log.error("Sign failed: Internal server error. (roomId: {})", requestDto.getRoomId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SignResponseDto.builder().isSuccess(false).data("500error").build());
        }
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


            // 대화 내역 조회
            String chatLog = chatPaser.buildSenderToContentsJsonByRoomId(roomId);

            // 상품 정보 조회
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

            //4. 서명 테이블에 초기 데이터 생성 (서명 상태: 양측 서명 대기)
            contractService.createInitialSignIfNotExists(roomId,product);
            return ResponseEntity.ok(successResponse);

        } catch (Exception e) {

            // 오류 발생 시, 서버 로그에만 상세 내용을 기록하기
            log.error("Failed to create contract from chat for roomId: {}", requestDto.getRoomId(), e);
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

