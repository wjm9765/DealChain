package com.dealchain.dealchain.domain.security;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * [보안] 데이터 무결성 검증을 위한 해시 생성 서비스
 * Java 시큐어 코딩 가이드에 따라 SHA-256 알고리즘을 사용합니다.
 */
@Service
public class HashService {

    private static final String HASH_ALGORITHM = "SHA-256";

    /**
     * roomId, userId, deviceInfo로 해시값 생성
     *
     * @param roomId     방 아이디 (null/빈값이면 IllegalArgumentException)
     * @param sellerId     판매자 ID (long)
     * @param buyerId     구매자 ID (long)
     * @return SHA-256 해시값 (16진수 문자열)
     */
    public String generateHashFromStrings(String roomId, long sellerId, long buyerId) {
        if (roomId == null || roomId.isBlank()) {
            throw new IllegalArgumentException("roomId는 비어있을 수 없습니다.");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);

            String dataToHash = String.join("::",
                    roomId,
                    String.valueOf(sellerId),
                    String.valueOf(buyerId)
            );

            byte[] encodedhash = digest.digest(dataToHash.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("해시값 생성에 실패했습니다.", e);
        }
    }

    /**
     * PDF 등 파일(MultipartFile)의 내용 전체를 기준으로 해시값을 생성합니다.
     */
    public String generateHashFromFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("해시를 생성할 파일이 비어있습니다.");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);

            try (InputStream inputStream = file.getInputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            byte[] encodedhash = digest.digest();
            return bytesToHex(encodedhash);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("해시값 생성에 실패했습니다.", e);
        } catch (IOException e) {
            throw new RuntimeException("파일 해시 생성 중 오류가 발생했습니다.", e);
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}