package com.dealchain.dealchain.domain.contract.service;

import com.dealchain.dealchain.domain.contract.entity.Contract;
import com.dealchain.dealchain.domain.contract.repository.ContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Transactional
public class ContractService {

    private final ContractRepository contractRepository;

    public ContractService(ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    /**
     * PDF 파일 경로를 받아 Contract 저장
     */
    public Contract saveContract(String filePath) {
        Contract contract = new Contract(filePath);
        return contractRepository.save(contract);
    }

    /**
     * ID로 Contract 조회 (파일 경로 반환)
     */
    @Transactional(readOnly = true)
    public Contract findById(UUID id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계약서입니다."));
    }

    /**
     * ID로 Contract 삭제 (DB 레코드와 파일 모두 삭제)
     */
    public void deleteContract(UUID id) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계약서입니다."));

        // 파일 삭제
        try {
            Path filePath = Paths.get(contract.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (Exception e) {
            // 파일 삭제 실패 시에도 DB 레코드는 삭제
            // 로그만 남기고 계속 진행
        }

        // DB 레코드 삭제
        contractRepository.delete(contract);
    }
}

