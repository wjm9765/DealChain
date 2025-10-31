package com.dealchain.dealchain.domain.contract;

import com.dealchain.dealchain.domain.security.S3UploadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
@Transactional(transactionManager = "contractTransactionManager")
public class ContractService {

    private final ContractRepository contractRepository;
    private final S3UploadService s3UploadService;

    public ContractService(ContractRepository contractRepository, S3UploadService s3UploadService) {
        this.contractRepository = contractRepository;
        this.s3UploadService = s3UploadService;
    }

    /**
     * PDF 파일을 S3에 업로드하고 경로를 RDS에 저장합니다.
     *
     * @param pdfFile 업로드할 PDF 파일
     * @return 저장된 Contract 엔티티
     */
    public Contract uploadAndSaveContract(MultipartFile pdfFile) {
        if (pdfFile == null || pdfFile.isEmpty()) {
            throw new IllegalArgumentException("PDF 파일이 제공되지 않았습니다.");
        }

        // S3에 PDF 업로드
        String filePath = s3UploadService.uploadPdf(pdfFile, "contracts/");

        // Contract 엔티티 생성 및 저장
        Contract contract = new Contract(filePath);
        return contractRepository.save(contract);
    }

    /**
     * ID로 Contract를 조회하고 S3에서 PDF 파일을 다운로드합니다.
     *
     * @param id Contract ID
     * @return Contract와 PDF 파일 정보를 담은 결과 객체
     */
    @Transactional(readOnly = true, transactionManager = "contractTransactionManager")
    public ContractPdfResult getContractPdf(Long id) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계약서입니다."));

        // S3에서 PDF 다운로드
        byte[] pdfBytes = s3UploadService.downloadFile(contract.getFilePath());

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

            // DB에서 Contract 삭제
            contractRepository.delete(contract);
        } catch (RuntimeException e) {
            // S3 삭제 실패 시에도 DB는 삭제하지 않도록 예외 전파
            // 또는 S3 삭제 실패를 무시하고 DB만 삭제할 수도 있습니다.
            throw new RuntimeException("계약서 삭제 중 오류가 발생했습니다: " + e.getMessage(), e);
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

