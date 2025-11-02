package com.dealchain.dealchain.domain.contract;

import com.dealchain.dealchain.domain.DealTracking.dto.DealTrackingRequest;
import com.dealchain.dealchain.domain.DealTracking.service.DealTrackingService;
import com.dealchain.dealchain.domain.chat.entity.ChatRoom;
import com.dealchain.dealchain.domain.chat.repository.ChatRoomRepository;
import com.dealchain.dealchain.domain.security.HashService;
import com.dealchain.dealchain.domain.security.S3UploadService;
import com.dealchain.dealchain.util.EncryptionUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
@Transactional(transactionManager = "contractTransactionManager")
public class ContractService {

    private final ContractRepository contractRepository;
    private final S3UploadService s3UploadService;
    private final HashService hashService;
    private final EncryptionUtil encryptionUtil;
    private final DealTrackingService dealTrackingService;
    private final ChatRoomRepository chatRoomRepository;

    public ContractService(ContractRepository contractRepository, 
                          S3UploadService s3UploadService,
                          HashService hashService,
                          EncryptionUtil encryptionUtil,
                          DealTrackingService dealTrackingService,
                          ChatRoomRepository chatRoomRepository) {
        this.contractRepository = contractRepository;
        this.s3UploadService = s3UploadService;
        this.hashService = hashService;
        this.encryptionUtil = encryptionUtil;
        this.dealTrackingService = dealTrackingService;
        this.chatRoomRepository = chatRoomRepository;
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

        // 해시값을 sellerId와 buyerId를 사용하여 암호화
        String encryptedHash;
        try {
            encryptedHash = encryptionUtil.encryptHashWithIds(hashValue, sellerId, buyerId);
        } catch (Exception e) {
            throw new RuntimeException("해시값 암호화 중 오류가 발생했습니다: " + e.getMessage(), e);
        }

        // Contract 엔티티 생성 및 저장
        Contract contract = new Contract(filePath, sellerId, buyerId, roomId, encryptedHash);
        Contract savedContract = contractRepository.save(contract);

        // DealTracking 기록 (CREATE)
        recordDealTracking(savedContract, "CREATE", null);

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

                // 3. 해시값 비교 (PDF 무결성 검증)
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
            // S3에서 파일 삭제
            s3UploadService.deleteFile(filePath);

            // DealTracking 기록 (DELETE) - 삭제 전에 기록
            recordDealTracking(contract, "DELETE", null);

            // DB에서 Contract 삭제
            contractRepository.delete(contract);
        } catch (RuntimeException e) {
            // S3 삭제 실패 시에도 DB는 삭제하지 않도록 예외 전파
            // 또는 S3 삭제 실패를 무시하고 DB만 삭제할 수도 있습니다.
            throw new RuntimeException("계약서 삭제 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 계약서 관련 작업을 DealTracking에 기록합니다.
     * 
     * @param contract 계약서 엔티티
     * @param type 작업 타입 (CREATE, READ, EDIT, DELETE)
     * @param deviceInfo 기기 정보 (선택사항)
     */
    private void recordDealTracking(Contract contract, String type, String deviceInfo) {
        try {
            // 인증된 사용자 정보 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                // 인증되지 않은 경우 tracking 기록하지 않음
                return;
            }

            String principalName = authentication.getName();
            Long principalId;
            try {
                principalId = Long.valueOf(principalName);
            } catch (NumberFormatException e) {
                // 사용자 ID 형식이 유효하지 않은 경우 기록하지 않음
                return;
            }

            // roomId 확인 (필수)
            if (contract.getRoomId() == null) {
                // roomId가 없는 경우 기록하지 않음
                return;
            }

            // role 결정 (현재 사용자가 seller인지 buyer인지 확인)
            String role = null;
            if (contract.getSellerId() != null && contract.getSellerId().equals(principalId)) {
                role = "SELLER";
            } else if (contract.getBuyerId() != null && contract.getBuyerId().equals(principalId)) {
                role = "BUYER";
            } else {
                // 사용자가 seller도 buyer도 아닌 경우 기록하지 않음
                return;
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
            System.err.println("DealTracking 기록 실패: " + e.getMessage());
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
                System.err.println("유효하지 않은 사용자임");
                return;
            }

            String principalName = authentication.getName();
            Long principalId;
            try {
                principalId = Long.valueOf(principalName);
            } catch (NumberFormatException e) {

                System.err.println("DealTracking 중단: 유효하지 않은 Principal ID (principalName: {})");
                return;
            }


            if (roomId == null || roomId.isEmpty()) {
                System.err.println("DealTracking 중단: roomId가 null이거나 비어있음 (userId: {})");
                return;
            }


            String role = null;
            if (sellerId != null && sellerId.equals(principalId)) {
                role = "SELLER";
            } else if (buyerId != null && buyerId.equals(principalId)) {
                role = "BUYER";
            } else {
                // 사용자가 해당 거래의 판매자도, 구매자도 아님 (인가(Authorization) 실패)
                System.out.println("DealTracking 중단: 사용자가 거래 당사자가 아님"+ principalId + roomId);
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
            System.err.println("DealTracking 기록 실패 (핵심 로직에 영향 없음): "+e.getMessage());

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

