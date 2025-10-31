package com.dealchain.dealchain.domain.contract.service;

import com.dealchain.dealchain.domain.contract.entity.Contract;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ContractService {

    private static final String UPLOAD_DIR = "uploads/contracts";
    private static final String INDEX_FILE = "uploads/contracts/contracts-index.csv"; // format: id,filePath\n

    /**
     * PDF 파일 경로를 받아 Contract 저장 (파일 기반 인덱스 사용)
     */
    public synchronized Contract saveContract(String filePath) {
        ensureUploadDir();
        UUID id = UUID.randomUUID();
        Contract contract = new Contract(filePath);
        contract.setId(id);
        Map<UUID, String> index = readIndex();
        index.put(id, filePath);
        writeIndex(index);
        return contract;
    }

    /**
     * ID로 Contract 조회 (파일 경로 반환)
     */
    @Transactional(readOnly = true)
    public synchronized Contract findById(UUID id) {
        Map<UUID, String> index = readIndex();
        String path = index.get(id);
        if (path == null) {
            throw new IllegalArgumentException("존재하지 않는 계약서입니다.");
        }
        Contract contract = new Contract(path);
        contract.setId(id);
        return contract;
    }

    /**
     * ID로 Contract 삭제 (파일과 인덱스 모두에서 제거)
     */
    public synchronized void deleteContract(UUID id) {
        Map<UUID, String> index = readIndex();
        String path = index.remove(id);
        if (path == null) {
            throw new IllegalArgumentException("존재하지 않는 계약서입니다.");
        }

        try {
            Path filePath = Paths.get(path);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (Exception ignored) {
        }

        writeIndex(index);
    }

    private void ensureUploadDir() {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path indexPath = Paths.get(INDEX_FILE).getParent();
            if (indexPath != null && !Files.exists(indexPath)) {
                Files.createDirectories(indexPath);
            }
            if (!Files.exists(Paths.get(INDEX_FILE))) {
                writeIndex(new HashMap<>());
            }
        } catch (IOException e) {
            throw new RuntimeException("업로드 디렉토리 초기화 중 오류", e);
        }
    }

    private Map<UUID, String> readIndex() {
        Map<UUID, String> map = new HashMap<>();
        Path indexPath = Paths.get(INDEX_FILE);
        if (!Files.exists(indexPath)) {
            return map;
        }
        try (BufferedReader reader = Files.newBufferedReader(indexPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                int comma = line.indexOf(',');
                if (comma <= 0) continue;
                String idStr = line.substring(0, comma).trim();
                String path = line.substring(comma + 1).trim();
                try {
                    UUID id = UUID.fromString(idStr);
                    map.put(id, path);
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("인덱스 파일 읽기 오류", e);
        }
        return map;
    }

    private void writeIndex(Map<UUID, String> index) {
        Path indexPath = Paths.get(INDEX_FILE);
        try (BufferedWriter writer = Files.newBufferedWriter(indexPath, StandardCharsets.UTF_8)) {
            for (Map.Entry<UUID, String> entry : index.entrySet()) {
                writer.write(entry.getKey().toString());
                writer.write(',');
                writer.write(entry.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("인덱스 파일 쓰기 오류", e);
        }
    }
}

