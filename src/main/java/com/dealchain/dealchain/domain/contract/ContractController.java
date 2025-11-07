package com.dealchain.dealchain.domain.contract;


import com.dealchain.dealchain.domain.chat.entity.ChatRoom;
import com.dealchain.dealchain.domain.chat.repository.ChatRoomRepository;
import com.dealchain.dealchain.domain.contract.dto.*;

import com.dealchain.dealchain.domain.contract.entity.ContractData;

import com.dealchain.dealchain.domain.contract.repository.ContractDataRepository;
import com.dealchain.dealchain.domain.contract.service.ContractService;
import com.dealchain.dealchain.domain.contract.service.JsonToPdfService;
import com.dealchain.dealchain.domain.member.Member;
import com.dealchain.dealchain.domain.member.MemberRepository;
import com.dealchain.dealchain.util.ByteArrayMultipartFile;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.dealchain.dealchain.util.EncryptionUtil;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {
    //    private final ChatPaser chatPaser;
//    private final AICreateContract AICreateContract;
//    private final ProductService productService;
    private final ChatRoomRepository chatRoomRepository;
    private final ContractService contractService;
    private final JsonToPdfService jsonToPdfService;
    private final MemberRepository memberRepository;
    private final ContractDataRepository contractDataRepository;
    private static final Logger log = LoggerFactory.getLogger(ContractController.class);
    private final EncryptionUtil encryptionUtil;

    public ContractController(ContractService contractService,
                              //AICreateContract aiCreateContract,
                              //ChatPaser chatPaser,
                              //ProductService productService,
                              ChatRoomRepository chatRoomRepository,
                              JsonToPdfService jsonToPdfService,
                              MemberRepository memberRepository,
                              ContractDataRepository contractDataRepository,
                              EncryptionUtil encryptionUtil
    ) {
        this.contractService = contractService;
        //this.AICreateContract = aiCreateContract;
        //this.chatPaser = chatPaser;
        //this.productService = productService;
        this.chatRoomRepository = chatRoomRepository;
        this.jsonToPdfService = jsonToPdfService;
        this.memberRepository = memberRepository;
        this.contractDataRepository = contractDataRepository;
        this.encryptionUtil = encryptionUtil;
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
            Optional<ChatRoom> roomOpt = chatRoomRepository.findById(roomId);
            if (roomOpt.isEmpty()) {
                SignResponseDto resp = SignResponseDto.builder()
                        .isSuccess(false)
                        .data("NotFound: 해당 roomId가 존재하지 않습니다.")
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
            }

            ChatRoom room = roomOpt.get();

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
                    requestDto.getContract(),
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
                String aiJson = requestDto.getContract(); // AI 생성 계약서 JSON,서비스 레이어에서 db와 값이 같은지 검증했기 때문에 그대로 사용

                // JSON과 서명 이미지로 PDF 생성
                byte[] pdfBytes = jsonToPdfService.createPdf(
                        aiJson,
                        sellerSignKey,
                        buyerSignKey
                );

                //메모리에 저장된 pdf를 MultipartFile로 변환
                MultipartFile finalPdfFile = new ByteArrayMultipartFile(
                        pdfBytes,
                        "contract-" + requestDto.getRoomId() + ".pdf",
                        "application/pdf"
                );


                try{
                    contractService.uploadAndSaveContract(finalPdfFile, sellerId, buyerId, roomId);

                }
                catch (Exception e){
                    log.error("PDF 업로드 및 저장 실패: roomId: {}, error: {}", requestDto.getRoomId(), e.getMessage());
                    response = SignResponseDto.builder()
                            .isSuccess(false)
                            .data("PDF 서버에 업로드 및 저장 중 오류가 발생했습니다.")
                            .bothSign(true)
                            .build();
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                }

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



    @PostMapping("/search")
    public ResponseEntity<ContractResponseDto> searchContract(
            @RequestBody ContractCreateRequestDto requestDto) {


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId; //현재 로그인한 사용자 ID
        try {
            currentUserId = Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            log.warn("인증 정보(JWT)에서 사용자 ID를 파싱할 수 없습니다: {}", authentication.getName());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ContractResponseDto.builder().isSuccess(false).data("유효하지 않은 인증 토큰입니다.").build());
        }

        try {
            String roomId = requestDto.getRoomId();

            // --------roomId가 있는지 DB에서 찾기 ---
            if (roomId == null || roomId.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ContractResponseDto.builder().isSuccess(false).data("roomId가 필요합니다.").build());
            }
            ChatRoom room = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방(roomId)입니다."));

            //DB에서 판매자 구매자 가져옴
            Long dbSellerId = room.getSellerId(); // (DB에서 가져옴)
            Long dbBuyerId = room.getBuyerId();   // (DB에서 가져옴)
            //교차 검증
            //boolean isSeller = currentUserId.equals(dbSellerId);
            boolean isBuyer = currentUserId.equals(dbBuyerId);

            //판매자가 서명 하고 구매자한테 계약서 전달 하는 시나리오만 고려
            if (!isBuyer) {
                log.warn("FORBIDDEN: User {} (JWT)가 roomId {}의 당사자(Seller: {})가 아닙니다.",
                        currentUserId, roomId, dbBuyerId);
                throw new SecurityException("이 계약서를 조회할 권한이 없습니다. (거래 당사자 아님)");
            }


            //ㄱ계약서 내용을 복호화해서 전달
            Optional<ContractData> contractDataOptional = contractDataRepository.findByRoomIdAndSellerIdAndBuyerId(roomId, dbSellerId, dbBuyerId);

            ContractData contractData = contractDataOptional.orElseThrow(
                    () -> new IllegalArgumentException("해당 조건의 계약서 데이터를 찾을 수 없습니다.")
            );

            String decryptedJson;
            try {
                decryptedJson = encryptionUtil.decryptString(contractData.getContractJsonData());

                if (decryptedJson == null) {
                    throw new IllegalStateException("복호화 결과가 null입니다.");
                }

            } catch (Exception e) {
                log.error("CRITICAL: 계약서 DB 복호화 실패! (ContractData ID: {})", contractData.getId(), e);
                throw new IllegalStateException("서버 오류: 계약서 데이터를 해독하는 데 실패했습니다.");
            }


            String Summary = contractService.getSummaryofContract(decryptedJson);

            ContractResponseDto successResponse = ContractResponseDto.builder()
                    .isSuccess(true)
                    .data(decryptedJson)
                    .summary(Summary)
                    .build();

            return ResponseEntity.ok(successResponse);

        } catch (IllegalArgumentException e) {
            // (Room ID를 못 찾은 경우)
            log.warn("Search failed: Resource not found. (roomId: {}) - {}", requestDto.getRoomId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ContractResponseDto.builder().isSuccess(false).data(e.getMessage()).build());

        } catch (SecurityException e) {
            // (권한이 없는 경우)
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ContractResponseDto.builder().isSuccess(false).data(e.getMessage()).build());

        } catch (Exception e) {
            // (그 외 모든 서버 오류)
            log.error("Search failed: Internal server error. (roomId: {})", requestDto.getRoomId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ContractResponseDto.builder().isSuccess(false).data("서버 내부 오류가 발생했습니다.").build());
        }
    }

    @GetMapping("/contractLists")
    public ResponseEntity<?> getContractLists( // 반환 타입을 '?' (와일드카드)로 변경
                                               //@RequestParam("roomId") String roomId,
                                               @RequestHeader(value = "User-Agent", defaultValue = "Unknown") String deviceInfo) {

        try {
            List<ContractInfoResponseDto> contractList = contractService.getMyContracts();

            return ResponseEntity.ok(contractList);

        } catch (SecurityException e) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN) // 403 Forbidden
                    .body(e.getMessage());

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST) // 400 Bad Request
                    .body(e.getMessage());

        } catch (Exception e) {
            // [서버 내부 오류] 그 외 모든 예외
            log.error("계약서 목록 조회 중 알 수 없는 오류 발생", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR) // 500 Internal Server Error
                    .body("서버 내부 오류가 발생했습니다.");
        }
    }
    //계약서 상세 조회
    @GetMapping("/detail")
    public ResponseEntity<?> getContractDetailByRoomId(
            @RequestParam("roomId") String roomId,
            @RequestHeader(value = "User-Agent", defaultValue = "Unknown") String deviceInfo) {

        try {
            // 1. [호출] roomId로 통합 서비스 호출
            ContractService.GetContractResponse response = contractService.getContractByRoomId(roomId, deviceInfo);

            // 2. [분기] 서비스가 반환한 DTO의 contentType을 확인
            if ("application/pdf".equals(response.getContentType())) {
                // [PDF 응답]
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                // "inline"은 브라우저에서 바로 열기, "attachment"는 다운로드
                headers.setContentDispositionFormData("inline", "contract_" + roomId + ".pdf");
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(response.getPdfBytes());
            } else {
                // [JSON 응답] (복호화된 JSON 문자열)
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response.getContractData());
            }

        } catch (SecurityException e) {
            // [인가 실패]
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            // [데이터 오류] (e.g., roomId 없음, 계약서 없음)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            // [서버 내부 오류] (e.g., 복호화 실패, S3 실패)
            log.error("계약서 상세 조회 중 알 수 없는 오류 발생 (RoomId: {})", roomId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류가 발생했습니다.");
        }
    }


    //계약서 거절 버튼 -> 서명 초기화
    @PostMapping("/reject")
    public ResponseEntity<SignResponseDto> rejectContract(
            @RequestBody ContractCreateRequestDto requestDto) {
        // roomId 유효성 검사 및 예외처리
        if (requestDto == null || requestDto.getRoomId() == null || requestDto.getRoomId().isEmpty()) {
            SignResponseDto errorResponse = SignResponseDto.builder()
                    .isSuccess(false).data("roomId가 유효하지 않습니다").build();
            return ResponseEntity.badRequest().body(errorResponse);
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            SignResponseDto resp = SignResponseDto.builder()
                    .isSuccess(false).data("Unauthorized: 인증 정보가 없습니다.").build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
        }
        Long currentUserId;
        try {
            currentUserId = Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            SignResponseDto resp = SignResponseDto.builder()
                    .isSuccess(false).data("Unauthorized: 사용자 ID 파싱 실패.").build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
        }

        try {
            //거절 요청 생성
            SignResponseDto responseDto = contractService.reject(requestDto, currentUserId);

            if (responseDto == null) {
                SignResponseDto resp = SignResponseDto.builder()
                        .isSuccess(false).data("서버 오류: 처리 결과가 없습니다.").build();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
            }

            if (responseDto.isSuccess()) {
                return ResponseEntity.ok(responseDto);
            } else {
                return ResponseEntity.badRequest().body(responseDto);
            }

        } catch (IllegalArgumentException e) {
            log.warn("Reject failed: Resource not found. (roomId: {}) - {}", requestDto.getRoomId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(SignResponseDto.builder().isSuccess(false).data(e.getMessage()).build());

        } catch (SecurityException e) {
            log.warn("Reject failed: Authorization failed. (roomId: {}) - {}", requestDto.getRoomId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(SignResponseDto.builder().isSuccess(false).data(e.getMessage()).build());

        } catch (Exception e) {
            log.error("Reject failed: Internal server error. (roomId: {})", requestDto.getRoomId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SignResponseDto.builder().isSuccess(false).data("서버 내부 오류가 발생했습니다.").build());
        }
    }

    @PostMapping("/edit")
    public ResponseEntity<ContractResponseDto> EditContract(
            @RequestBody ContractEditRequestDto requestDto) {
        // roomId 유효성 검사 및 예외처리
        if (requestDto == null || requestDto.getRoomId() == null || requestDto.getRoomId().isEmpty()) {
            ContractResponseDto errorResponse = ContractResponseDto.builder()
                    .isSuccess(false).data("roomId가 유효하지 않습니다").build();
            return ResponseEntity.badRequest().body(errorResponse);
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            ContractResponseDto resp = ContractResponseDto.builder()
                    .isSuccess(false).data("Unauthorized: 인증 정보가 없습니다.").build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
        }
        Long currentUserId;
        try {
            currentUserId = Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            ContractResponseDto resp = ContractResponseDto.builder()
                    .isSuccess(false).data("Unauthorized: 사용자 ID 파싱 실패.").build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
        }
        try {
            // 편집 요청 처리, 거래 추적도 생성됨
            ContractResponseDto responseDto = contractService.EditContract(requestDto, currentUserId);

            if (responseDto == null) {
                ContractResponseDto resp = ContractResponseDto.builder()
                        .isSuccess(false).data("서버 오류: 처리 결과가 없습니다.").build();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
            }

            if (responseDto.isSuccess()) {
                return ResponseEntity.ok(responseDto);
            } else {
                return ResponseEntity.badRequest().body(responseDto);
            }
        } catch (IllegalArgumentException e) {
            log.warn("Edit failed: Resource not found. (roomId: {}) - {}", requestDto.getRoomId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ContractResponseDto.builder().isSuccess(false).data(e.getMessage()).build());

        } catch (SecurityException e) {
            log.warn("Edit failed: Authorization failed. (roomId: {}) - {}", requestDto.getRoomId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ContractResponseDto.builder().isSuccess(false).data(e.getMessage()).build());

        } catch (Exception e) {
            log.error("Edit failed: Internal server error. (roomId: {})", requestDto.getRoomId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ContractResponseDto.builder().isSuccess(false).data("서버 내부 오류가 발생했습니다.").build());
        }
    }

    @PostMapping("/send")
    public ResponseEntity<SignResponseDto> sendToBuyer(
            @RequestBody ContractCreateRequestDto requestDto) {

        if (requestDto == null || requestDto.getRoomId() == null || requestDto.getRoomId().isEmpty()) {
            SignResponseDto errorResponse = SignResponseDto.builder()
                    .isSuccess(false)
                    .data("roomId가 유효하지 않습니다")
                    .bothSign(false)
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            SignResponseDto resp = SignResponseDto.builder()
                    .isSuccess(false)
                    .data("Unauthorized: 인증 정보가 없습니다.")
                    .bothSign(false)
                    .build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
        }

        Long currentUserId;
        try {
            currentUserId = Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            SignResponseDto resp = SignResponseDto.builder()
                    .isSuccess(false)
                    .data("Unauthorized: 사용자 ID 파싱 실패.")
                    .bothSign(false)
                    .build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
        }

        try {
            SignResponseDto responseDto = contractService.sendTobuyerService(requestDto, currentUserId);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(responseDto);

        } catch (SecurityException e) {
            log.warn("FORBIDDEN: User {} tried to send contract for room {} without auth.", currentUserId, requestDto.getRoomId(), e);
            SignResponseDto errorResponse = SignResponseDto.builder()
                    .isSuccess(false)
                    .data(e.getMessage())
                    .bothSign(false)
                    .build();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);

        } catch (IllegalArgumentException e) {
            log.warn("BAD_REQUEST: User {} failed to send contract for room {}: {}", currentUserId, requestDto.getRoomId(), e.getMessage());
            SignResponseDto errorResponse = SignResponseDto.builder()
                    .isSuccess(false)
                    .data(e.getMessage())
                    .bothSign(false)
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Failed to send contract for roomId: {}", requestDto.getRoomId(), e);
            SignResponseDto errorResponse = SignResponseDto.builder()
                    .isSuccess(false)
                    .data("서버 내부 오류가 발생했습니다.")
                    .bothSign(false)
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @PostMapping("/create")
    public ResponseEntity<ContractResponseDto> createContractFromChat(
            @RequestBody ContractCreateRequestDto requestDto) {

        if (requestDto == null || requestDto.getRoomId() == null || requestDto.getRoomId().isEmpty()) {
            ContractResponseDto errorResponse = ContractResponseDto.builder()
                    .isSuccess(false).data(null).build();
            return ResponseEntity.badRequest().body(errorResponse);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.valueOf(authentication.getName());

        try {

            ContractResponseDto responseDto = contractService.createContract(
                    requestDto,
                    currentUserId
            );

            return ResponseEntity.ok(responseDto);

        } catch (SecurityException e) {
            log.warn("FORBIDDEN: User {} tried to create contract for room {} without auth.",
                    currentUserId, requestDto.getRoomId(), e);
            ContractResponseDto errorResponse = ContractResponseDto.builder()
                    .isSuccess(false).data(e.getMessage()).build();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);

        } catch (IllegalArgumentException e) {
            log.warn("BAD_REQUEST: User {} failed to create contract for room {}: {}",
                    currentUserId, requestDto.getRoomId(), e.getMessage());
            ContractResponseDto errorResponse = ContractResponseDto.builder()
                    .isSuccess(false).data(e.getMessage()).build();
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Failed to create contract from chat for roomId: {}", requestDto.getRoomId(), e);
            ContractResponseDto errorResponse = ContractResponseDto.builder()
                    .isSuccess(false).data(null).build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }



    @PostMapping("/summary")
    public ResponseEntity<ContractSummaryDto> createContractSummary(
            @RequestBody ContractCreateRequestDto requestDto) {

        if (requestDto == null || requestDto.getRoomId() == null || requestDto.getRoomId().isEmpty()) {
            ContractSummaryDto errorResponse = ContractSummaryDto.builder()
                    .isSuccess(false).data(null).build();
            return ResponseEntity.badRequest().body(errorResponse);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.valueOf(authentication.getName());

        try {

            ContractSummaryDto responseDto = contractService.summaryContract(
                    requestDto,
                    currentUserId
            );

            return ResponseEntity.ok(responseDto);

        } catch (SecurityException e) {
            log.warn("FORBIDDEN: User {} tried to create contract for room {} without auth.",
                    currentUserId, requestDto.getRoomId(), e);
            ContractSummaryDto errorResponse = ContractSummaryDto.builder()
                    .isSuccess(false).data(e.getMessage()).build();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);

        } catch (IllegalArgumentException e) {
            log.warn("BAD_REQUEST: User {} failed to create contract for room {}: {}",
                    currentUserId, requestDto.getRoomId(), e.getMessage());
            ContractSummaryDto errorResponse = ContractSummaryDto.builder()
                    .isSuccess(false).data(e.getMessage()).build();
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Failed to create contract from chat for roomId: {}", requestDto.getRoomId(), e);
            ContractSummaryDto errorResponse = ContractSummaryDto.builder()
                    .isSuccess(false).data(null).build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    @PostMapping("/reason")
    public ResponseEntity<ContractReasonDto> createContractReason(
            @RequestBody ContractCreateRequestDto requestDto) {

        if (requestDto == null || requestDto.getRoomId() == null || requestDto.getRoomId().isEmpty()) {
            ContractReasonDto errorResponse = ContractReasonDto.builder()
                    .isSuccess(false).data(null).build();
            return ResponseEntity.badRequest().body(errorResponse);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.valueOf(authentication.getName());

        try {

            ContractReasonDto responseDto = contractService.reasonContract(
                    requestDto,
                    currentUserId
            );

            return ResponseEntity.ok(responseDto);

        } catch (SecurityException e) {
            log.warn("FORBIDDEN: User {} tried to create contract for room {} without auth.",
                    currentUserId, requestDto.getRoomId(), e);
            ContractReasonDto errorResponse = ContractReasonDto.builder()
                    .isSuccess(false).data(e.getMessage()).build();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);

        } catch (IllegalArgumentException e) {
            log.warn("BAD_REQUEST: User {} failed to create contract for room {}: {}",
                    currentUserId, requestDto.getRoomId(), e.getMessage());
            ContractReasonDto errorResponse = ContractReasonDto.builder()
                    .isSuccess(false).data(e.getMessage()).build();
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Failed to create contract from chat for roomId: {}", requestDto.getRoomId(), e);
            ContractReasonDto errorResponse = ContractReasonDto.builder()
                    .isSuccess(false).data(null).build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }


}

