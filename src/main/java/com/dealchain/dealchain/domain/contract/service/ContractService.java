package com.dealchain.dealchain.domain.contract.service;

import com.dealchain.dealchain.domain.AI.dto.ContractDefaultReqeustDto;
import com.dealchain.dealchain.domain.chat.entity.ChatNotification;
import com.dealchain.dealchain.domain.chat.repository.ChatNotificationRepository;
import com.dealchain.dealchain.domain.contract.dto.*;
import com.dealchain.dealchain.domain.contract.repository.ContractDataRepository;
import com.dealchain.dealchain.domain.member.Member;
import com.dealchain.dealchain.domain.member.MemberRepository;
import com.dealchain.dealchain.domain.security.XssSanitizer;
import com.dealchain.dealchain.util.EncryptionUtil;
import com.dealchain.dealchain.domain.AI.service.AICreateContract;
import com.dealchain.dealchain.domain.AI.service.AIHelpService;
import com.dealchain.dealchain.domain.AI.service.ChatPaser;
import com.dealchain.dealchain.domain.DealTracking.dto.DealTrackingRequest;
import com.dealchain.dealchain.domain.DealTracking.service.DealTrackingService;
import com.dealchain.dealchain.domain.chat.entity.ChatRoom;
import com.dealchain.dealchain.domain.chat.repository.ChatRoomRepository;
import com.dealchain.dealchain.domain.contract.entity.ContractData;
import com.dealchain.dealchain.domain.contract.repository.SignRepository;
import com.dealchain.dealchain.domain.contract.entity.Contract;
import com.dealchain.dealchain.domain.contract.ContractRepository;
import com.dealchain.dealchain.domain.contract.entity.SignTable;
import com.dealchain.dealchain.domain.product.Product;
import com.dealchain.dealchain.domain.product.ProductService;
import com.dealchain.dealchain.domain.security.HashService;
import com.dealchain.dealchain.domain.security.S3UploadService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(transactionManager = "contractTransactionManager")
public class ContractService {
    private static final Logger log = LoggerFactory.getLogger(ContractService.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;//알림을 위한 의존성

    private final ContractRepository contractRepository;
    private final S3UploadService s3UploadService;
    private final HashService hashService;
    private final EncryptionUtil encryptionUtil;
    private final DealTrackingService dealTrackingService;
    private final ChatRoomRepository chatRoomRepository;
    private final SignRepository signRepository;
    private final AIHelpService aiHelpService;
    private final ChatPaser chatPaser;
    private final AICreateContract aiCreateContract;
    private final ProductService productService;
    private final ContractDataRepository contractDataRepository;
    private final ObjectMapper objectMapper;
    private final MemberRepository memberRepository;
    private final ChatNotificationRepository chatNotificationRepository;
    private final XssSanitizer xssSanitizer;

    public ContractService(ContractRepository contractRepository, 
                          S3UploadService s3UploadService,
                          HashService hashService,
                          EncryptionUtil encryptionUtil,
                          DealTrackingService dealTrackingService,
                          SignRepository signRepository,
                          ChatRoomRepository chatRoomRepository,
                           ChatPaser chatPaser,
                           AICreateContract aiCreateContract,
                            ProductService productService,
                           ContractDataRepository contractDataRepository,
                           MemberRepository memberRepository,
                           ObjectMapper objectMapper,
                           ChatNotificationRepository chatNotificationRepository,
                            XssSanitizer xssSanitizer,
                           AIHelpService aiHelpService) {
        this.contractRepository = contractRepository;
        this.s3UploadService = s3UploadService;
        this.hashService = hashService;
        this.encryptionUtil = encryptionUtil;
        this.dealTrackingService = dealTrackingService;
        this.chatRoomRepository = chatRoomRepository;
        this.signRepository= signRepository;
        this.aiHelpService=aiHelpService;
        this.chatPaser = chatPaser;
        this.aiCreateContract = aiCreateContract;
        this.productService = productService;
        this.contractDataRepository = contractDataRepository;
        this.objectMapper=objectMapper;
        this.memberRepository = memberRepository;
        this.chatNotificationRepository = chatNotificationRepository;
        this.xssSanitizer = xssSanitizer;
    }

    public String getSummaryofContract(String contract){
        return aiHelpService.invokeClaude(contract);
    }

    @Transactional
    public SignResponseDto sendTobuyerService(ContractCreateRequestDto requestDto,Long currentUserId) throws Exception{
        String roomId = requestDto.getRoomId();
        Optional<Long> productIdOpt = chatRoomRepository.findProductIdByRoomId(roomId);
        Long productId = productIdOpt.orElseThrow(
                () -> new IllegalArgumentException("해당 roomId에 대한 productId가 없습니다. roomId=" + roomId)
        );
        Product product = productService.findById(productId);

        Long sellerId = product.getMemberId();
        Long buyerId = chatRoomRepository.findBuyerIdByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("해당 roomId에 대한 buyerId가 없습니다. roomId=" + roomId));

        if(requestDto.getBuyerId()!=buyerId||requestDto.getSellerId()!=sellerId){
            throw new IllegalArgumentException("요청에 들어있는 값들이 실제 회원 정보와 일치하지 않습니다.");
        }

        //판매자가 구매자에게 서명을 요청할 수 있음
        if (!currentUserId.equals(sellerId)) {
            throw new SecurityException("판매자 본인만 서명을 요청할 수 있습니다.");
        }

        //1. 현재 판매자가 서명한 상태여야함.
        Optional<SignTable> signTableOpt = signRepository.findByRoomIdAndProductId(roomId, productId);
        if (signTableOpt.isEmpty()) {
            throw new IllegalArgumentException("SignTable이 존재하지 않습니다.");
        }
        SignTable signTable = signTableOpt.get();
        if(signTable.getStatus()!= SignTable.SignStatus.PENDING_BUYER){
            //판매자가 서명한 상태가 아니면 예외
            throw new IllegalArgumentException("판매자가 서명을 해야합니다.");
        }
        //2. 구매자에게 서명 요청 알림 전송

        sendNotification(
                buyerId,
                sellerId,
                roomId,
                "서명 요청이 있습니다.",
                "SIGN_REQUEST",
                null
        );//

        //거래 추적 테이블 작성 SIGN_REQUEST
        recordDealTrackingForCreate("SIGN_REQUEST",requestDto.getRoomId(),sellerId,buyerId,requestDto.getDeviceInfo());

        return SignResponseDto.builder()
                .isSuccess(true)
                .data("구매자한테 서명 요청을 보냈습니다.")
                .bothSign(false)
                .build();
    }

    @Transactional
    public SignResponseDto reject(ContractCreateRequestDto requestDto,Long currentUserId) throws Exception{
        //구매자가 서명을 거절했을 때
        //requestDto에 들어있는 값들이 실제로 존재하는지 검증 roomId,sellerId,buyerId
        //signtable에서 undoSignbySeller 호출 -> 판매자가 서명 호출한 걸 무효화 처리
        //sendContractRequestNotification 호출 -> 판매자에게 알림 전송(sellerId,message: "구매자가 계약서 서명을 거절했습니다.")

        String roomId = requestDto.getRoomId();
        Optional<Long> productIdOpt = chatRoomRepository.findProductIdByRoomId(roomId);
        Long productId = productIdOpt.orElseThrow(
                () -> new IllegalArgumentException("해당 roomId에 대한 productId가 없습니다. roomId=" + roomId)
        );
        Product product = productService.findById(productId);

        Long sellerId = product.getMemberId();
        Long buyerId = chatRoomRepository.findBuyerIdByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("해당 roomId에 대한 buyerId가 없습니다. roomId=" + roomId));

        if(requestDto.getBuyerId()!=buyerId||requestDto.getSellerId()!=sellerId){
            throw new IllegalArgumentException("요청에 들어있는 값들이 실제 회원 정보와 일치하지 않습니다.");
        }

        // 구매자만 거절 할 수 있도록 요청
        if (!currentUserId.equals(buyerId)) {
            throw new SecurityException("구매자 본인만 서명을 거절할 수 있습니다.");
        }

        //SignTable 조회 및 판매자 서명 무효화 ---
        SignTable signTable = signRepository.findByRoomIdAndProductId(roomId, productId)
                .orElseThrow(() -> new IllegalArgumentException("SignTable이 존재하지 않습니다."));

        signTable.undoSignBySeller();
        signRepository.save(signTable); // 변경 사항 저장

        //판매자에게 '거절' 알림 전송

        sendNotification(
                sellerId,
                buyerId,
                roomId,
                "계약서 서명을 거절했습니다.",
                "CONTRACT_REJECT",
                null
        );//
        //거래 추적 테이블 작성
        recordDealTrackingForCreate("REJECT",requestDto.getRoomId(),sellerId,buyerId,requestDto.getDeviceInfo());

        return SignResponseDto.builder()
                .isSuccess(true)
                .data("계약서 서명이 거절되었으며, 판매자에게 알림을 전송했습니다.")
                .bothSign(false)
                .build();
    }

    @Transactional
    public ContractResponseDto EditContract(ContractEditRequestDto requestDto, Long currentUserId) throws Exception {

        //값이 맞는지 검증 조회
        String roomId = requestDto.getRoomId();
        Optional<Long> productIdOpt = chatRoomRepository.findProductIdByRoomId(roomId);
        Long productId = productIdOpt.orElseThrow(
                () -> new IllegalArgumentException("해당 roomId에 대한 productId가 없습니다. roomId=" + roomId)
        );
        Product product = productService.findById(productId);
        Long sellerId = product.getMemberId(); // (DB의 '진짜' 판매자 ID)

        //판매자만 계약서 수정할 수 있음
        if (!currentUserId.equals(sellerId)) {
            throw new SecurityException("판매자만 계약서를 수정할 수 있습니다.");
        }

        // 구매자 ID 조회
        Long buyerId = chatRoomRepository.findBuyerIdByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("해당 roomId에 대한 buyerId가 없습니다. roomId=" + roomId));

        //수정된 계약서 업데이트
        ContractData contractData = contractDataRepository.findByRoomIdAndSellerIdAndBuyerId(roomId, sellerId, buyerId)
                .orElseThrow(() -> new IllegalArgumentException("수정할 기존 계약서 데이터를 찾을 수 없습니다."));


        // 2b. 요약본 생성
        String newSummary = getSummaryofContract(requestDto.getEditjson());


        // 새로운 계약서 내용을 암호화
        String encryptedJson;
        try {
            encryptedJson = encryptionUtil.encryptString(requestDto.getEditjson());
        } catch (Exception e) {
            log.error("계약서 (수정) 암호화 실패: {}", e.getMessage(), e);
            throw new RuntimeException("계약서 암호화 중 오류가 발생했습니다.", e);
        }
        contractData.updateContractJson(encryptedJson);

        // 거래 추적 ---
        recordDealTrackingForCreate(
                "EDIT", // 추적 유형 변경
                roomId,
                sellerId,
                buyerId,
                requestDto.getDeviceInfo()
        );

        //변경된 데이터 저장
        contractDataRepository.save(contractData);
        // --- 5. [응답] ---
        // 수정된 '새' 계약서와 '새' 요약본을 반환
        return ContractResponseDto.builder()
                .isSuccess(true)
                .data(requestDto.getEditjson())
                .summary(newSummary)
                .build();
    }

    @Transactional
    public ContractResponseDto createContract(ContractCreateRequestDto requestDto, Long currentUserId) {

        String roomId = requestDto.getRoomId();
        Optional<Long> productIdOpt = chatRoomRepository.findProductIdByRoomId(roomId);
        Long productId = productIdOpt.orElseThrow(
                () -> new IllegalArgumentException("해당 roomId에 대한 productId가 없습니다. roomId=" + roomId)
        );
        Product product = productService.findById(productId);

        Long sellerId = product.getMemberId(); // (DB의 '진짜' 판매자 ID)
        Long buyerId = chatRoomRepository.findBuyerIdByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("해당 roomId에 대한 buyerId가 없습니다. roomId=" + roomId));

        boolean isCallerSeller = currentUserId.equals(sellerId);
        boolean isCallerBuyer = currentUserId.equals(buyerId);

        if (!isCallerSeller && !isCallerBuyer) {
            throw new SecurityException("당신은 이 거래의 당사자가 아닙니다.");
        }

        if(isCallerBuyer) {//만약 구매자가 요청했으면 AI 호출하지 않고 바로 return
            sendNotification(
                    sellerId,
                    buyerId,
                    roomId,
                     "계약서 검토 요청이 있습니다.",
                    "CONTRACT_REQUEST",
                    null
            );//구매자에게 알림 전송
            return ContractResponseDto.builder()
                    .isSuccess(true)
                    .data("계약서 생성 요청을 판매자에게 보냈습니다.")
                    .summary(null)
                    .build();
        }
        String chatLog = chatPaser.buildSenderToContentsJsonByRoomId(roomId);


        //id로 각 회원정보에 저장되어 있는 '이름'을 가져옴
        String sellerName = memberRepository.findNameById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("판매자 정보를 찾을 수 없습니다. sellerId=" + sellerId));
        String buyerName = memberRepository.findNameById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("구매자 정보를 찾을 수 없습니다. buyerId=" + buyerId));



        ContractDefaultReqeustDto default_request = ContractDefaultReqeustDto.builder()
                .seller_id(sellerId).buyer_id(buyerId)
                .seller_name(sellerName).buyer_name(buyerName).product(product).build();


        String aiContractJson = aiCreateContract.invokeClaude(chatLog, default_request);
        String summary = getSummaryofContract(aiContractJson);//요약 버전

        // --- 3. [DB 저장] (Transaction) ---

        // 3a. 거래 추적 (DTO의 ID 대신 '신뢰' ID 사용)
        recordDealTrackingForCreate("CREATE", roomId, sellerId, buyerId, requestDto.getDeviceInfo());

        //초기 서명 테이블 생성,both_pending
        createInitialSignIfNotExists(roomId, product);

        String encryptedJson;
        try {
            encryptedJson = encryptionUtil.encryptString(aiContractJson);
        } catch (Exception e) {
            log.error("계약서 암호화 실패: {}", e.getMessage(), e);
            throw new RuntimeException("계약서 암호화 중 오류가 발생했습니다.", e);
        }

        // 3d. 'ContractData' 엔티티 생성 및 저장
        ContractData contractDataToSave = ContractData.builder()
                .roomId(roomId)
                .sellerId(sellerId)
                .buyerId(buyerId)
                .contractJsonData(encryptedJson)
                .build();
        contractDataRepository.save(contractDataToSave); //계약서 저장


        if (isCallerSeller) {//판매자가 생성을 요청했을 시
            return ContractResponseDto.builder()
                    .isSuccess(true)
                    .data(aiContractJson)
                    .summary(summary)
                    .build();

        }
        else{
            throw new SecurityException("알 수 없는 에러 발생");
        }
    }
//
//    //판매자가 서명 완료하고 구매자한테 서명 요청하는것
//    @Async
//    public void sendSignContractRequestNotification(Long sellerId, String roomId, Long buyerId) {
//        try {
//            Map<String, String> notificationPayload = Map.of(
//                    "type", "CONTRACT_REQUEST",
//                    "message", " 계약서 서명 요청이 있습니다.",
//                    "roomId", roomId
//            );
//            // [핵심] '판매자'의 '개인 알림 채널'로 메시지 전송
//            messagingTemplate.convertAndSendToUser(
//                    String.valueOf(buyerId),
//                    "/queue/notifications",
//                    notificationPayload
//            );
//
//            log.info("판매자(ID: {})에게 계약서 서명 요청 알림 전송 완료 (RoomId: {})", sellerId, roomId);
//
//        } catch (Exception e) {
//            log.warn("WebSocket 알림 전송 실패 (계약서 생성은 성공함): {}", e.getMessage());
//        }
//    }
//
//    //구매자가 계약서 생성 요청을 날림
//    @Async
//    public void sendContractRequestNotification(Long sellerId, String roomId, Long buyerId,String message) {
//        try {
//            Map<String, String> notificationPayload = Map.of(
//                    "type", "CONTRACT_REQUEST",
//                    "message", message,
//                    "roomId", roomId
//            );
//            // [핵심] '판매자'의 '개인 알림 채널'로 메시지 전송
//            messagingTemplate.convertAndSendToUser(
//                    String.valueOf(sellerId),
//                    "/queue/notifications",
//                    notificationPayload
//            );
//
//            log.info("판매자(ID: {})에게 계약서 검토 요청 알림 전송 완료 (RoomId: {})", sellerId, roomId);
//
//        } catch (Exception e) {
//            log.warn("WebSocket 알림 전송 실패 (계약서 생성은 성공함): {}", e.getMessage());
//        }
//    }

    //개인 알림 채널로 보냄
    //위의 두개를 이걸로 통합할 것
    @Async
    @Transactional
    public void sendNotification(Long who,Long sender, String roomId,String message,String type,String AIcontent) {

        //AIcontent = AI가 왜 그렇게 판단했는지 근거가 되는 json 형태
        try {
            Map<String, String> notificationPayload = Map.of(
                    "type", type,
                    "message", message,
                    "roomId", roomId
            );
            //알림 전송
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(who),
                    "/queue/notifications",
                    notificationPayload
            );

            ChatNotification chatNotification = ChatNotification.builder()
                    .memberId(who)
                    .senderId(sender)
                    .roomId(roomId)
                    .type(ChatNotification.NotificationType.valueOf(type))
                    .AIContent(AIcontent)
                    .build();

            chatNotificationRepository.save(chatNotification);

            log.info("(ID: {})에게 {}: {}  알림 전송 완료 (RoomId: {})", who,type,message, roomId);

        } catch (Exception e) {
            log.warn("WebSocket 알림 전송 실패: {}", e.getMessage());
        }
    }

    /**
     * 계약서 생성 시 초기 서명 테이블 생성 (중복 방지)
     */
    @Transactional
    public SignTable createInitialSignIfNotExists(String roomId, Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product가 필요합니다.");
        }
        // 중복 방지
        Optional<SignTable> existing = signRepository.findByRoomIdAndProductId(roomId, product.getId());
        if (existing.isPresent()) {
            return existing.get(); // 이미 있으면 기존 객체 반환
        }

        SignTable signTable = SignTable.builder()
                .roomId(roomId)
                .productId(product.getId())
                .build();

        return signRepository.save(signTable); // 저장 후 영속화된 엔티티 반환
    }



    /**
     * 계약서 서명 처리 (양측 서명 완료 시 PDF 생성 필요)
     */
    @Transactional
    public SignResponseDto signContract(String roomId, Long productId, Long userId, String role,String contract, String deviceInfo) {
        // 필수 파라미터 검사 (null/빈 문자열 포함)
        if (roomId == null || roomId.isBlank()
                || productId == null
                || userId == null
                || role == null || role.isBlank()
                || deviceInfo == null || deviceInfo.isBlank()
                || contract == null || contract.isEmpty()) {
            return SignResponseDto.builder()
                    .isSuccess(false)
                    .data("BadRequest: 필수 파라미터 누락")
                    .build();
        }

        // 저장된 계약서 조회 (Optional 안전 처리)
        ContractData contractData = contractDataRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("해당 roomId에 대한 계약서 데이터가 없습니다. roomId=" + roomId));

        // 복호화는 예외 처리
        String decryptedContractJson;
        try {
            decryptedContractJson = encryptionUtil.decryptString(contractData.getContractJsonData());
        } catch (Exception e) {
            log.error("계약서 복호화 실패: {}", e.getMessage(), e);
            return SignResponseDto.builder()
                    .isSuccess(false)
                    .data("ServerError: 계약서 복호화 실패")
                    .build();
        }

        // 비교: 저장된 계약서와 전달된 계약서가 동일한지 확인
        //수정이 아니라 서명이라서 내용이 같아야 함


        try {
            //  DB의 JSON 문자열을 JsonNode 객체로 변환
            JsonNode dbContractNode = objectMapper.readTree(decryptedContractJson);
            //  클라이언트가 전달한 JSON 문자열을 JsonNode 객체로 변환
            JsonNode clientContractNode = objectMapper.readTree(contract);
            //  두 JSON 객체의 구조와 값이 완전히 동일한지 비교 수행
            if (!dbContractNode.equals(clientContractNode)) {
                log.warn("계약서 서명 시도 중 내용 불일치 감지. RoomId: {}", roomId);
                return SignResponseDto.builder()
                        .isSuccess(false)
                        .data("BadRequest: 저장된 계약서 내용이 다릅니다.")
                        .build();
            }
        } catch (JsonProcessingException e) {
            //  만약 전달된 'contract' 문자열이 유효한 JSON이 아니라면 파싱 예외 발생
            log.error("계약서 JSON 파싱 실패 (RoomId: {}): {}", roomId, e.getMessage());
            return SignResponseDto.builder()
                    .isSuccess(false)
                    .data("ServerError: 계약서 데이터(JSON) 파싱에 실패했습니다.")
                    .build();
        }


        //role은 SELLER,BUYER
        //1. 기존 서명 테이블에 있는 항목을 불러옴
        Optional<SignTable> signOpt = signRepository.findByRoomIdAndProductId(roomId, productId);
        if (signOpt.isEmpty()) {
            return SignResponseDto.builder()
                    .isSuccess(false)
                    .data("NotFound: SignTable이 존재하지 않습니다.")
                    .build();
        }
        SignTable signTable = signOpt.get();

        //2. 서명 상태 업데이트 (엔티티에 있는 함수 사용) JPA가 자동 업데이트
        // 역할 판별 (대소문자 무시)
        if ("SELLER".equalsIgnoreCase(role)) {
            signTable.signBySeller();
        } else if ("BUYER".equalsIgnoreCase(role)) {
            signTable.signByBuyer();
        } else {
            return SignResponseDto.builder()
                    .isSuccess(false)
                    .data("BadRequest: role은 SELLER 또는 BUYER 여야 합니다.")
                    .build();
        }
        // 양측 서명 완료 여부 확인
        boolean BothSign = false;
        if (signTable.isCompleted()) {
            BothSign = true; // 양측 서명 완료 시 PDF 생성 필요
        }
        signRepository.save(signTable);


        //4. 거래 추적 테이블 작성
        DealTrackingRequest request = DealTrackingRequest.builder()
                .roomId(String.valueOf(roomId))
                .role(role)
                .deviceInfo(deviceInfo)
                .build();
        dealTrackingService.dealTrack("SIGN", request);

        return SignResponseDto.builder()
                .isSuccess(true)
                .data("서명이 성공적으로 처리되었습니다.")
                .bothSign(BothSign)
                .build();
    }



    /**
     * PDF 파일을 S3에 업로드하고 경로를 RDS에 저장합니다.
     *
     * @param pdfFile  업로드할 PDF 파일
     * @param sellerId 판매자 ID
     * @param buyerId  구매자 ID
     * @param roomId   채팅방 ID
     * @return 저장된 Contract 엔티티
     */
    public Contract uploadAndSaveContract(MultipartFile pdfFile, Long sellerId, Long buyerId, String roomId) {
        if (pdfFile == null || pdfFile.isEmpty()) {
            throw new IllegalArgumentException("PDF 파일이 제공되지 않았습니다.");
        }

        if (sellerId == null || buyerId == null) {
            throw new IllegalArgumentException("sellerId와 buyerId는 필수입니다.");
        }

        // roomId가 제공된 경우 chat DB에 존재하는지 확인
        if (roomId != null && !roomId.isEmpty()) {
            Optional<ChatRoom> chatRoom = chatRoomRepository.findById(roomId);
            if (chatRoom.isEmpty()) {
                throw new IllegalArgumentException("존재하지 않는 채팅방(roomId)입니다.");
            }
        }

        // S3에 PDF 업로드
        String filePath = s3UploadService.uploadPdf(pdfFile, "contracts/");

        // PDF 파일의 내용으로부터 해시값 생성
        String hashValue = hashService.generateHashFromFile(pdfFile);

        // PDF 무결성 검증을 위한 해시값 암호화 (sellerId, buyerId 사용)
        String encryptedHash;
        try {
            encryptedHash = encryptionUtil.encryptHashWithIds(hashValue, sellerId, buyerId);
        } catch (Exception e) {
            throw new RuntimeException("해시값 암호화 중 오류가 발생했습니다: " + e.getMessage(), e);
        }

        // Contract 엔티티 생성 및 저장
        Contract contract = new Contract(filePath, sellerId, buyerId, roomId, encryptedHash);
        Contract savedContract = contractRepository.save(contract);

        Optional<ContractData> contractDataOptional = contractDataRepository.findByRoomIdAndSellerIdAndBuyerId(roomId, sellerId, buyerId);
        contractDataOptional.ifPresent(cd -> {
            try {
                // 서명 테이블은 근거를 위해 유지
                contractDataRepository.delete(cd);
            } catch (Exception e) {
                log.warn("ContractData 삭제 실패 roomId={} sellerId={} buyerId={} : {}", roomId, sellerId, buyerId, e.getMessage());
            }
        });

        // DealTracking 기록 (SAVE)
        recordDealTracking(savedContract, "SAVE", null);

        return savedContract;
    }
//
//
//    /**
//     * ID로 Contract의 PDF를 교체합니다. (같은 경로로 업로드하여 덮어씁니다)
//     *
//     * @param id      Contract ID
//     * @param pdfFile 새로운 PDF 파일
//     * @return 업데이트된 Contract 엔티티
//     */
//    public Contract updateContractPdf(Long id, MultipartFile pdfFile) {
//        if (pdfFile == null || pdfFile.isEmpty()) {
//            throw new IllegalArgumentException("PDF 파일이 제공되지 않았습니다.");
//        }
//
//        Contract contract = contractRepository.findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계약서입니다."));
//
//        String existingFilePath = contract.getFilePath();
//
//        // S3의 같은 경로에 새로운 PDF 업로드 (기존 파일 자동 덮어쓰기)
//        s3UploadService.uploadPdfToPath(pdfFile, existingFilePath);
//
//        // PDF 파일의 내용으로부터 새로운 해시값 생성 및 암호화
//        String hashValue = hashService.generateHashFromFile(pdfFile);
//        String encryptedHash;
//        try {
//            encryptedHash = encryptionUtil.encryptHashWithIds(hashValue, contract.getSellerId(), contract.getBuyerId());
//        } catch (Exception e) {
//            throw new RuntimeException("해시값 암호화 중 오류가 발생했습니다: " + e.getMessage(), e);
//        }
//        contract.setEncryptedHash(encryptedHash);
//
//        // DB의 filePath와 encryptedHash 업데이트
//        Contract updatedContract = contractRepository.save(contract);
//
//        // DealTracking 기록 (EDIT)
//        recordDealTracking(updatedContract, "EDIT", null);
//
//        return updatedContract;
//    }

    /**
     * ID로 Contract를 삭제합니다. (DB와 S3에서 모두 삭제)
     *
     * @param id Contract ID
     * @throws IllegalArgumentException 존재하지 않는 계약서인 경우
     */
    public void deleteContract(Long id) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계약서입니다."));

        String filePath = contract.getFilePath();

        try {
            s3UploadService.deleteFile(filePath);
            recordDealTracking(contract, "DELETE", null);
            contractRepository.delete(contract);
        } catch (RuntimeException e) {
            // S3 삭제 실패 시에도 DB는 삭제하지 않도록 예외 전파
            throw new RuntimeException("계약서 삭제 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 계약서 작업 추적 기록 (거래 당사자만 기록, 실패해도 계약서 작업은 계속)
     */
    private void recordDealTracking(Contract contract, String type, String deviceInfo) {
        try {
            // 인증 및 권한 검증
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return;
            }

            String principalName = authentication.getName();
            Long principalId;
            try {
                principalId = Long.valueOf(principalName);
            } catch (NumberFormatException e) {
                return;
            }

            if (contract.getRoomId() == null) {
                return;
            }

            // 거래 당사자(SELLER/BUYER) 확인
            String role = null;
            if (contract.getSellerId() != null && contract.getSellerId().equals(principalId)) {
                role = "SELLER";
            } else if (contract.getBuyerId() != null && contract.getBuyerId().equals(principalId)) {
                role = "BUYER";
            } else {
                return; // 거래 당사자가 아닌 경우 기록하지 않음
            }

            // DealTrackingRequest 생성
            DealTrackingRequest request = DealTrackingRequest.builder()
                    .roomId(String.valueOf(contract.getRoomId()))
                    .role(role)
                    .deviceInfo(deviceInfo)
                    .build();

            // DealTracking 기록
            dealTrackingService.dealTrack(type, request);
        } catch (Exception e) {
            // DealTracking 기록 실패는 로그만 남기고 예외를 전파하지 않음
            // (계약서 작업 자체는 성공해야 하므로)
            log.error("DealTracking 기록 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * '계약서 생성' 이벤트에 대한 거래 추적을 기록합니다.
     * Contract 객체 생성 전에 호출되며, 주요 ID와 deviceInfo를 인자로 받습니다.
     *
     * @param type       추적 유형 (예: "CREATE_DRAFT")
     * @param roomId     거래 채팅방 ID
     * @param sellerId   판매자 ID
     * @param buyerId    구매자 ID
     * @param deviceInfo 요청 디바이스 정보
     */
     public void recordDealTrackingForCreate(String type, String roomId, Long sellerId, Long buyerId, String deviceInfo) {
        try {
            // 인증 정보 확인
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("유효하지 않은 사용자임");
                return;
            }

            String principalName = authentication.getName();
            Long principalId;
            try {
                principalId = Long.valueOf(principalName);
            } catch (NumberFormatException e) {

                log.warn("DealTracking 중단: 유효하지 않은 Principal ID (principalName: {})", principalName);
                return;
            }


            if (roomId == null || roomId.isEmpty()) {
                log.warn("DealTracking 중단: roomId가 null이거나 비어있음 (principalId: {})", principalId);
                return;
            }


            String role = null;
            if (sellerId != null && sellerId.equals(principalId)) {
                role = "SELLER";
            } else if (buyerId != null && buyerId.equals(principalId)) {
                role = "BUYER";
            } else {
                // 사용자가 해당 거래의 판매자도, 구매자도 아님 (인가(Authorization) 실패)
                log.warn("DealTracking 중단: 사용자가 거래 당사자가 아님 (principalId: {}, roomId: {})", principalId, roomId);
                return;
            }

            // 4. DealTrackingRequest 생성 (인자 사용)
            DealTrackingRequest request = DealTrackingRequest.builder()
                    .roomId(roomId)
                    .role(role)
                    .deviceInfo(deviceInfo)
                    .build();

            // 5. DealTracking 기록
            dealTrackingService.dealTrack(type, request);

        } catch (Exception e) {

            // 예외를 전파하지 않고 경고 로그만 남깁니다.
            log.error("DealTracking 기록 실패 (핵심 로직에 영향 없음): {}", e.getMessage(), e);

        }
    }

    //계약서 목록 반환

    @Transactional(readOnly = true, transactionManager = "contractTransactionManager")
    public List<ContractInfoResponseDto> getMyContracts() {

        Long currentUserId = getCurrentUserIdFromSecurityContext();
        List<ContractInfoResponseDto> responseList = new ArrayList<>();

        // 1. '작성 중'인 계약 목록 (ContractData DB)
        List<ContractData> pendingContracts = contractDataRepository.findBySellerIdOrBuyerId(currentUserId, currentUserId);

        for (ContractData data : pendingContracts) {
            responseList.add(ContractInfoResponseDto.builder()
                    .roomId(data.getRoomId())
                    .contractId(null) // '작성 중'이므로 PDF ID는 없음
                    .contractDataId(data.getId()) // "contract data 객체의 id"
                    .status(getSignStatusForRoom(data.getRoomId())) // PENDING_*
                    .build());
        }

        // 2. '완료'된 계약 목록 (Contract DB)
        List<Contract> completedContracts = contractRepository.findBySellerIdOrBuyerId(currentUserId, currentUserId);
        for (Contract contract : completedContracts) {
            responseList.add(ContractInfoResponseDto.builder()
                    .roomId(contract.getRoomId())
                    .contractId(contract.getId()) // "contract 객체의 Id"
                    .contractDataId(null) // '완료'되었으므로 JSON ID는 null (혹은 필요시 둘 다)
                    .status(SignTable.SignStatus.COMPLETED) // 이 목록은 완료된 것들
                    .build());
        }

        return responseList;
    }
    /**
     * [헬퍼] 헬퍼 메소드 추가 (코드 중복 제거)
     */
    private Long getCurrentUserIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new SecurityException("인증된 사용자가 아닙니다.");
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new SecurityException("유효하지 않은 사용자 인증 정보입니다.");
        }
    }

    /**
     * [헬퍼] RoomID로 서명 상태 조회
     */
    private SignTable.SignStatus getSignStatusForRoom(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            log.warn("roomId가 없어 서명 상태를 조회할 수 없습니다.");
            return null;
        }

        Long productId = chatRoomRepository.findProductIdByRoomId(roomId)
                .orElse(null);

        if (productId == null) {
            log.warn("RoomId: {}의 productId가 없어 서명 상태를 조회할 수 없습니다.", roomId);
            return null;
        }

        return signRepository.findByRoomIdAndProductId(roomId, productId)
                .map(SignTable::getStatus)
                .orElse(null);
    }


    @Transactional(readOnly = true, transactionManager = "contractTransactionManager")
    public GetContractResponse getContractByRoomId(String roomId, String deviceInfo) {
        Long currentUserId = getCurrentUserIdFromSecurityContext();
        //roomID로 판매자 구매자 조회
        Long productId = chatRoomRepository.findProductIdByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("해당 roomId에 대한 productId가 없습니다. roomId=" + roomId));

        Product product = productService.findById(productId);
        Long sellerId = product.getMemberId();
        Long buyerId = chatRoomRepository.findBuyerIdByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("해당 roomId에 대한 buyerId가 없습니다. roomId=" + roomId));

        if (!currentUserId.equals(sellerId) && !currentUserId.equals(buyerId)) {
            log.warn("인가 실패: 사용자가 해당 채팅방의 당사자가 아님 (UserId: {}, RoomId: {})", currentUserId, roomId);
            throw new SecurityException("이 계약서에 접근할 권한이 없습니다.");
        }

        // 1. 서명 상태 조회
        SignTable signTable = signRepository.findByRoomIdAndProductId(roomId, productId)
                .orElseThrow(() -> new IllegalStateException("계약서의 서명 상태 정보를 찾을 수 없습니다. (RoomId: " + roomId + ")"));

        SignTable.SignStatus status = signTable.getStatus();

        // 2.  서명 상태에 따라 반환 데이터 결정
        if (status == SignTable.SignStatus.COMPLETED) {
            // [기능 1] 서명 완료: Contract(PDF) 조회
            Contract contract = contractRepository.findByRoomId(roomId)
                    .orElseThrow(() -> new IllegalStateException("완료된 계약서 정보를 찾을 수 없습니다. (RoomId: " + roomId + ")"));

            byte[] pdfBytes = s3UploadService.downloadFile(contract.getFilePath());

            //혹시 수정 된게 있는지 해쉬값으로 검증
            if (contract.getEncryptedHash() != null && !contract.getEncryptedHash().isBlank()) {
                try {
                    String decryptedHash = encryptionUtil.decryptHashWithIds(
                            contract.getEncryptedHash(),
                            contract.getSellerId(),
                            contract.getBuyerId()
                    );
                    String currentHash = hashService.generateHashFromBytes(pdfBytes);

                    if (!decryptedHash.equals(currentHash)) {
                        log.warn("계약서 무결성 검증 실패! (ContractId: {})", contract.getId());
                        throw new IllegalArgumentException("계약서 파일의 무결성 검증에 실패했습니다. 파일이 변조되었을 수 있습니다.");
                    }
                } catch (IllegalArgumentException e) {
                    throw e;
                } catch (Exception e) {
                    log.error("해시값 검증 중 암호화 오류 발생 (ContractId: {}): {}", contract.getId(), e.getMessage(), e);
                    throw new RuntimeException("해시값 검증 중 오류가 발생했습니다: " + e.getMessage(), e);
                }
            }

            recordDealTracking(contract, "READ_BOTH_SIGN", deviceInfo); // PDF 조회 추적

            return new GetContractResponse(status, pdfBytes); // PDF 반환

        } else {
            // [기능 2] 서명 미완료 (작성 중): ContractData(JSON) 조회
            ContractData contractData = contractDataRepository.findByRoomId(roomId)
                    .orElseThrow(() -> new IllegalStateException("작성 중인 계약서 데이터를 찾을 수 없습니다. (RoomId: " + roomId + ")"));

            String encryptedJson = contractData.getContractJsonData();
            String decryptedJson;
            try {
                decryptedJson = encryptionUtil.decryptString(encryptedJson);
            } catch (Exception e) {
                log.error("계약서(JSON) 복호화 실패 (RoomId: {}): {}", roomId, e.getMessage(), e);
                throw new RuntimeException("계약서 데이터를 처리하는 중 오류가 발생했습니다.", e);
            }

            // DealTracking 기록 (READ_DRAFT)
            recordDealTrackingForCreate("READ_NOT_BOTH_SIGN", roomId, sellerId, buyerId, deviceInfo);

            String summary = getSummaryofContract(decryptedJson);
            return new GetContractResponse(status, decryptedJson,summary); // JSON 반환
        }
    }

    /**
     * Contract와 PDF 파일 정보를 담는 결과 클래스, 일반 json도 할 수 있게
     */
    public static class GetContractResponse {
        private final SignTable.SignStatus signStatus;
        private final String contentType; // "application/pdf" 또는 "application/json"
        private final byte[] pdfBytes;
        private final String contractData; // 복호화된 JSON 문자열
        private final String summary;

        // PDF 반환용 생성자 (서명 완료) - summary는 null
        public GetContractResponse(SignTable.SignStatus status, byte[] pdfBytes) {
            this.signStatus = status;
            this.contentType = "application/pdf";
            this.pdfBytes = pdfBytes;
            this.contractData = null;
            this.summary = null;
        }


        // JSON 반환용 생성자 (작성 중) - summary 지정 가능 (null 허용)
        public GetContractResponse(SignTable.SignStatus status, String contractData, String summary) {
            this.signStatus = status;
            this.contentType = "application/json";
            this.pdfBytes = null;
            this.contractData = contractData;
            this.summary = summary;
        }

        public SignTable.SignStatus getSignStatus() {
            return signStatus;
        }

        public String getContentType() {
            return contentType;
        }

        public byte[] getPdfBytes() {
            return pdfBytes;
        }

        public String getContractData() {
            return contractData;
        }

        public String getSummary() {
            return summary;
        }
    }
}

