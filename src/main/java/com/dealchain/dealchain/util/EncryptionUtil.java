package com.dealchain.dealchain.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@Component
public class EncryptionUtil {
    
    @Value("${encryption.secret-key}")
    private String secretKeyString;
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";
    
    private SecretKeySpec getSecretKey() {
        byte[] key = secretKeyString.getBytes();
        // AES 키는 16, 24, 또는 32 바이트여야 함
        byte[] keyBytes = new byte[32];
        System.arraycopy(key, 0, keyBytes, 0, Math.min(key.length, 32));
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
    
    /**
     * 파일을 암호화하여 저장
     * @param originalFilePath 원본 파일 경로
     * @param encryptedFilePath 암호화된 파일 저장 경로
     * @throws Exception
     */
    public void encryptFile(String originalFilePath, String encryptedFilePath) throws Exception {
        Path originalPath = Paths.get(originalFilePath);
        Path encryptedPath = Paths.get(encryptedFilePath);
        
        // 암호화된 파일의 디렉토리 생성
        Files.createDirectories(encryptedPath.getParent());
        
        try (FileInputStream fis = new FileInputStream(originalPath.toFile());
             FileOutputStream fos = new FileOutputStream(encryptedPath.toFile())) {
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
            
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] encryptedBytes = cipher.update(buffer, 0, bytesRead);
                if (encryptedBytes != null) {
                    fos.write(encryptedBytes);
                }
            }
            
            byte[] finalBytes = cipher.doFinal();
            if (finalBytes != null) {
                fos.write(finalBytes);
            }
        }
    }
    
    /**
     * 암호화된 파일을 복호화하여 저장
     * @param encryptedFilePath 암호화된 파일 경로
     * @param decryptedFilePath 복호화된 파일 저장 경로
     * @throws Exception
     */
    public void decryptFile(String encryptedFilePath, String decryptedFilePath) throws Exception {
        Path encryptedPath = Paths.get(encryptedFilePath);
        Path decryptedPath = Paths.get(decryptedFilePath);
        
        // 복호화된 파일의 디렉토리 생성
        Files.createDirectories(decryptedPath.getParent());
        
        try (FileInputStream fis = new FileInputStream(encryptedPath.toFile());
             FileOutputStream fos = new FileOutputStream(decryptedPath.toFile())) {
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey());
            
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] decryptedBytes = cipher.update(buffer, 0, bytesRead);
                if (decryptedBytes != null) {
                    fos.write(decryptedBytes);
                }
            }
            
            byte[] finalBytes = cipher.doFinal();
            if (finalBytes != null) {
                fos.write(finalBytes);
            }
        }
    }
    
    /**
     * 파일을 메모리에서 암호화
     * @param fileBytes 원본 파일 바이트 배열
     * @return 암호화된 파일 바이트 배열
     * @throws Exception
     */
    public byte[] encryptFileBytes(byte[] fileBytes) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
        return cipher.doFinal(fileBytes);
    }
    
    /**
     * 암호화된 파일 바이트 배열을 복호화
     * @param encryptedBytes 암호화된 파일 바이트 배열
     * @return 복호화된 파일 바이트 배열
     * @throws Exception
     */
    public byte[] decryptFileBytes(byte[] encryptedBytes) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey());
        return cipher.doFinal(encryptedBytes);
    }
    
    /**
     * 문자열을 암호화하여 Base64로 인코딩된 문자열 반환
     * @param plainText 원본 문자열
     * @return 암호화된 Base64 문자열
     * @throws Exception
     */
    public String encryptString(String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }
    
    /**
     * Base64로 인코딩된 암호화 문자열을 복호화
     * @param encryptedText 암호화된 Base64 문자열
     * @return 복호화된 원본 문자열
     * @throws Exception
     */
    public String decryptString(String encryptedText) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey());
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes, "UTF-8");
    }
}
