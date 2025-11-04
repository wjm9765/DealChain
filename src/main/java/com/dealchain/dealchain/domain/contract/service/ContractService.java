package com.dealchain.dealchain.domain.contract.service;

import com.dealchain.dealchain.domain.DealTracking.dto.DealTrackingRequest;
import com.dealchain.dealchain.domain.DealTracking.service.DealTrackingService;
import com.dealchain.dealchain.domain.chat.entity.ChatRoom;
import com.dealchain.dealchain.domain.chat.repository.ChatRoomRepository;
import com.dealchain.dealchain.domain.contract.SignRepository;
import com.dealchain.dealchain.domain.contract.dto.SignResponseDto;
import com.dealchain.dealchain.domain.contract.entity.Contract;
import com.dealchain.dealchain.domain.contract.ContractRepository;
import com.dealchain.dealchain.domain.contract.entity.SignTable;
import com.dealchain.dealchain.domain.product.Product;
import com.dealchain.dealchain.domain.security.HashService;
import com.dealchain.dealchain.domain.security.S3UploadService;
import com.dealchain.dealchain.util.EncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
@Transactional(transactionManager = "contractTransactionManager")
public class ContractService {
    private static final Logger log = LoggerFactory.getLogger(ContractService.class);

    private final ContractRepository contractRepository;
    private final S3UploadService s3UploadService;
    private final HashService hashService;
    private final EncryptionUtil encryptionUtil;
    private final DealTrackingService dealTrackingService;
    private final ChatRoomRepository chatRoomRepository;
    private final SignRepository signRepository;

    public ContractService(ContractRepository contractRepository, 
                          S3UploadService s3UploadService,
                          HashService hashService,
                          EncryptionUtil encryptionUtil,
                          DealTrackingService dealTrackingService,
                          SignRepository signRepository,
                          ChatRoomRepository chatRoomRepository) {
        this.contractRepository = contractRepository;
        this.s3UploadService = s3UploadService;
        this.hashService = hashService;
        this.encryptionUtil = encryptionUtil;
        this.dealTrackingService = dealTrackingService;
        this.chatRoomRepository = chatRoomRepository;
        this.signRepository= signRepository;
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
    public SignResponseDto signContract(String roomId, Long productId, Long userId, String role, String deviceInfo) {
        if (roomId == null || roomId.isBlank() || productId == null || userId == null || role == null||deviceInfo==null) {
            return SignResponseDto.builder()
                    .isSuccess(false)
                    .data("BadRequest: 필수 파라미터 누락")
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

        // DealTracking 기록 (SAVE)
        recordDealTracking(savedContract, "SAVE", null);

        return savedContract;
    }




    /**
     * ID로 Contract를 조회하고 S3에서 PDF 파일을 다운로드합니다.
     * 저장된 해시값과 다운로드한 PDF의 해시값을 비교하여 무결성을 검증합니다.
     *
     * @param id Contract ID
     * @return Contract와 PDF 파일 정보를 담은 결과 객체
     * @throws IllegalArgumentException 계약서가 없거나 해시값이 일치하지 않는 경우
     */
    @Transactional(readOnly = true, transactionManager = "contractTransactionManager")
    public ContractPdfResult getContractPdf(Long id) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계약서입니다."));

        if (contract.getSellerId() == null || contract.getBuyerId() == null) {
            throw new IllegalArgumentException("sellerId와 buyerId가 없는 계약서는 조회할 수 없습니다.");
        }

        // S3에서 PDF 다운로드
        byte[] pdfBytes = s3UploadService.downloadFile(contract.getFilePath());

        // 저장된 해시값 복호화 및 검증
        if (contract.getEncryptedHash() != null && !contract.getEncryptedHash().isBlank()) {
            try {
                // 1. DB에 저장된 암호화된 해시값 복호화
                String decryptedHash = encryptionUtil.decryptHashWithIds(
                        contract.getEncryptedHash(), 
                        contract.getSellerId(), 
                        contract.getBuyerId()
                );

                // 2. 다운로드한 PDF 파일로부터 해시값 생성
                String currentHash = hashService.generateHashFromBytes(pdfBytes);

                // 해시값 비교로 PDF 무결성 검증
                if (!decryptedHash.equals(currentHash)) {
                    throw new IllegalArgumentException("계약서 파일의 무결성 검증에 실패했습니다. 파일이 변조되었을 수 있습니다.");
                }
            } catch (IllegalArgumentException e) {
                throw e; // 무결성 검증 실패는 그대로 전파
            } catch (Exception e) {
                throw new RuntimeException("해시값 검증 중 오류가 발생했습니다: " + e.getMessage(), e);
            }
        }

        // DealTracking 기록 (READ)
        recordDealTracking(contract, "READ", null);

        return new ContractPdfResult(contract, pdfBytes);
    }

    /**
     * 첫 번째 계약서의 PDF를 조회합니다. (임시 PDF 반환용)
     *
     * @return Contract와 PDF 파일 정보를 담은 결과 객체
     */
    @Transactional(readOnly = true, transactionManager = "contractTransactionManager")
    public ContractPdfResult getFirstContractPdf() {
        Optional<Contract> contractOpt = contractRepository.findAll().stream().findFirst();

        Contract contract = contractOpt.orElseThrow(
                () -> new IllegalArgumentException("저장된 계약서가 없습니다."));

        // S3에서 PDF 다운로드
        byte[] pdfBytes = s3UploadService.downloadFile(contract.getFilePath());

        return new ContractPdfResult(contract, pdfBytes);
    }

    /**
     * ID로 Contract의 PDF를 교체합니다. (같은 경로로 업로드하여 덮어씁니다)
     *
     * @param id      Contract ID
     * @param pdfFile 새로운 PDF 파일
     * @return 업데이트된 Contract 엔티티
     */
    public Contract updateContractPdf(Long id, MultipartFile pdfFile) {
        if (pdfFile == null || pdfFile.isEmpty()) {
            throw new IllegalArgumentException("PDF 파일이 제공되지 않았습니다.");
        }

        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계약서입니다."));

        String existingFilePath = contract.getFilePath();

        // S3의 같은 경로에 새로운 PDF 업로드 (기존 파일 자동 덮어쓰기)
        s3UploadService.uploadPdfToPath(pdfFile, existingFilePath);

        // PDF 파일의 내용으로부터 새로운 해시값 생성 및 암호화
        String hashValue = hashService.generateHashFromFile(pdfFile);
        String encryptedHash;
        try {
            encryptedHash = encryptionUtil.encryptHashWithIds(hashValue, contract.getSellerId(), contract.getBuyerId());
        } catch (Exception e) {
            throw new RuntimeException("해시값 암호화 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
        contract.setEncryptedHash(encryptedHash);

        // DB의 filePath와 encryptedHash 업데이트
        Contract updatedContract = contractRepository.save(contract);

        // DealTracking 기록 (EDIT)
        recordDealTracking(updatedContract, "EDIT", null);

        return updatedContract;
    }

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

    /**
     * Contract와 PDF 파일 정보를 담는 결과 클래스
     */
    public static class ContractPdfResult {
        private final Contract contract;
        private final byte[] pdfBytes;

        public ContractPdfResult(Contract contract, byte[] pdfBytes) {
            this.contract = contract;
            this.pdfBytes = pdfBytes;
        }

        public Contract getContract() {
            return contract;
        }

        public byte[] getPdfBytes() {
            return pdfBytes;
        }
    }
}

