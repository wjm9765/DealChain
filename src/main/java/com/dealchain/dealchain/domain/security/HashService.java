package com.dealchain.dealchain.domain.security;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

/**
 * [보안] 데이터 무결성 검증을 위한 해시 생성 서비스
 * Java 시큐어 코딩 가이드에 따라 SHA-256 알고리즘을 사용합니다.
 */
@Service
public class HashService {

    private static final String HASH_ALGORITHM = "SHA-256";

    /**
     * [함수 1] 여러 텍스트 입력을 조합하여 해시값을 생성합니다.
     * (예: 거래 추적 로그 생성 시)
     *
     * @param userId     사용자 ID
     * @param timestamp  시간
     * @param deviceInfo 기기 번호 (또는 User-Agent)
     * @param otherData  추가 데이터 (예: actionType)
     * @return SHA-256 해시값 (16진수 문자열)
     */
    public String generateHashFromStrings(String userId, LocalDateTime timestamp, String deviceInfo, String otherData) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);

            // [보안] 입력값들을 '::' 같은 고유 구분자(Delimiter)로 명확히 분리합니다.
            // (그냥 합치면 "USER1" + "TIME10"과 "USER1TIME" + "10"이 같아지는 충돌이 생길 수 있습니다)
            String dataToHash = String.join("::",
                    userId,
                    timestamp.toString(),
                    deviceInfo,
                    otherData
            );

            // 해시 계산
            byte[] encodedhash = digest.digest(
                    dataToHash.getBytes(StandardCharsets.UTF_8));

            // 바이트 배열을 16진수 문자열로 변환
            return bytesToHex(encodedhash);

        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 Java 표준 알고리즘이므로 이 예외는 거의 발생하지 않습니다.
            //log.error("{} 알고리즘을 찾을 수 없습니다.", HASH_ALGORITHM, e);
            throw new RuntimeException("해시값 생성에 실패했습니다.", e);
        }
    }

    /**
     * [함수 2] PDF 등 파일(MultipartFile)의 내용 전체를 기준으로 해시값을 생성합니다.
     * (예: 전자서명된 계약서 PDF 파일의 원본 증명 시)
     *
     * @param file (PDF, 이미지 등)
     * @return 파일 내용의 SHA-256 해시값 (16진수 문자열)
     */
    public String generateHashFromFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("해시를 생성할 파일이 비어있습니다.");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);

            // [성능] 파일을 한 번에 메모리에 올리지 않고, 조금씩(8KB 버퍼) 읽어 해시를 업데이트합니다.
            // C++의 'fread'와 유사한 방식입니다. 대용량 파일도 메모리 문제없이 처리 가능합니다.
            try (InputStream inputStream = file.getInputStream()) {
                byte[] buffer = new byte[8192]; // 8KB 버퍼
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            // 최종 해시값 계산
            byte[] encodedhash = digest.digest();

            // 바이트 배열을 16진수 문자열로 변환
            return bytesToHex(encodedhash);

        } catch (NoSuchAlgorithmException e) {
            //log.error("{} 알고리즘을 찾을 수 없습니다.", HASH_ALGORITHM, e);
            throw new RuntimeException("해시값 생성에 실패했습니다.", e);
        } catch (IOException e) {
            //log.error("파일을 읽는 중 I/O 오류가 발생했습니다.", e);
            throw new RuntimeException("파일 해시 생성 중 오류가 발생했습니다.", e);
        }
    }


    /**
     * (공용 헬퍼 함수) 바이트 배열을 16진수(Hex) 문자열로 변환합니다.
     */
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