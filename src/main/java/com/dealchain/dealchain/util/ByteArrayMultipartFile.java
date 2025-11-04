package com.dealchain.dealchain.util;

import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * [보안] 'byte[]' (메모리)를 'MultipartFile' (웹) 인터페이스로 변환하는 어댑터.
 * 'java 시큐어 코딩 가이드' (84p. 임시 파일 관리)를 준수하기 위해
 * '서버 디스크'에 임시 파일을 절대 생성하지 않습니다.
 */
public class ByteArrayMultipartFile implements MultipartFile {

    private final byte[] bytes;
    private final String name;
    private final String originalFilename;
    private final String contentType;

    /**
     * @param bytes          파일의 실제 바이트 배열 (RAM)
     * @param originalFilename "contract.pdf"와 같은 파일명
     * @param contentType    "application/pdf"와 같은 MIME 타입
     */
    public ByteArrayMultipartFile(byte[] bytes, String originalFilename, String contentType) {
        this.bytes = bytes;
        this.name = "file"; // (파라미터 이름은 중요하지 않음)
        this.originalFilename = originalFilename;
        this.contentType = contentType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return bytes == null || bytes.length == 0;
    }

    @Override
    public long getSize() {
        return bytes.length;
    }

    @Override
    public byte[] getBytes() throws IOException {
        // [핵심] 1. 원본 byte[]를 그대로 반환
        return bytes;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        // [핵심] 2. byte[]를 '메모리 스트림'으로 감싸서 반환
        return new ByteArrayInputStream(bytes);
    }

    /**
     * [보안 경고] 이 메서드는 '임시 디스크 파일'을 생성하므로 절대 사용해선 안 됩니다.
     * 호출 시 즉시 예외를 발생시켜 '보안 가이드'를 강제합니다.
     */
    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        // [보안] 'java 시큐어 코딩 가이드' (84p) 위반 방지
        throw new IllegalStateException("transferTo()는 임시 파일을 생성하므로 보안상 금지됩니다.");
    }
}