package com.dealchain.dealchain.domain.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3UploadService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucket;

    /**
     * S3에 파일을 업로드하고, 저장된 고유 키(경로)를 반환합니다.
     *
     * @param file          업로드할 MultipartFile
     * @param directoryPath S3 버킷 내의 디렉토리 경로 (예: "profiles/")
     * @return S3에 저장된 파일의 고유 키 (예: "profiles/uuid-filename.jpg")
     * @throws IOException
     */

    //db에 암호화해서 저장해야됨
    public String upload(MultipartFile file, String directoryPath) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("S3 버킷 이름(`aws.s3.bucket-name`)이 설정되어 있지 않습니다.");
        }

        validateFile(file);

        //파일 이름을 uuid로 랜덤으로 저장
        String uniqueFileName = directoryPath + UUID.randomUUID().toString() + "-" + (file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());

        long contentLength = file.getSize();

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(uniqueFileName)
                    .contentType(file.getContentType())
                    .build();

            if (contentLength <= 0) {
                // content length가 불명확한 경우 안전하게 바이트로 읽어 업로드
                byte[] bytes = file.getBytes(); // IOException 가능
                s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));
            } else {
                try (InputStream inputStream = file.getInputStream()) {
                    s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, contentLength));
                }
            }

            return uniqueFileName;
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            // S3 서비스 에러 (권한, 버킷 존재 여부, 리전 불일치 등)
            throw new RuntimeException("S3 업로드 실패 - 코드: " + e.statusCode() + ", 메시지: " + e.getMessage(), e);
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            // 네트워크/클라이언트 설정 문제
            throw new RuntimeException("AWS SDK 클라이언트 오류: " + e.getMessage(), e);
        } catch (IOException e) {
            // 파일 읽기 실패
            throw new RuntimeException("업로드할 파일을 읽는 중 오류가 발생했습니다: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            // validateFile 등에서 발생한 입력값 오류
            throw e;
        } catch (Exception e) {
            // 알 수 없는 예외
            throw new RuntimeException("알 수 없는 업로드 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * [보안] Private S3 객체에 대한 임시 접근 URL(Pre-signed URL)을 생성합니다.
     *
     * @param fileKey DB에 저장된 파일의 고유 키 (예: "profiles/uuid-filename.jpg")
     * @param minutes URL 만료 시간 (분 단위)
     * @return 임시 접근 가능한 URL
     */

    //임시 접근 키를 발급 -> db에 저장하면 안됨 !!
    public String generatePresignedUrl(String fileKey, int minutes) {
        if (fileKey == null || fileKey.isEmpty()) {
            return null; // 파일 키가 없는 경우 null 반환 (또는 기본 이미지 URL)
        }

        try {
            // 1. URL을 발급받을 '파일' 지정
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileKey)
                    .build();

            // 2. URL의 '유효 시간' 설정
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(minutes))
                    .getObjectRequest(getObjectRequest)
                    .build();

            // 3. S3Presigner로 URL '발급'
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

            // 4. 발급된 URL(임시 출입증)을 문자열로 반환
            return presignedRequest.url().toString();

        } catch (Exception e) {

            throw new RuntimeException("Pre-signed URL 생성에 실패했습니다.", e);
        }
    }

    /**
     * S3에서 파일을 다운로드하여 바이트 배열로 반환합니다.
     *
     * @param fileKey S3 버킷 내의 파일 키 (경로) (예: "signatures/uuid-filename.jpg")
     * @return 파일의 바이트 배열
     * @throws RuntimeException 파일이 존재하지 않거나 다운로드 중 오류가 발생한 경우
     */
    public byte[] downloadFile(String fileKey) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("S3 버킷 이름(`aws.s3.bucket-name`)이 설정되어 있지 않습니다.");
        }

        if (fileKey == null || fileKey.isEmpty()) {
            throw new IllegalArgumentException("파일 키가 제공되지 않았습니다.");
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileKey)
                    .build();

            try (ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(getObjectRequest)) {
                return responseInputStream.readAllBytes();
            }
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            throw new RuntimeException("S3에서 파일을 찾을 수 없습니다: " + fileKey, e);
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            throw new RuntimeException("S3 다운로드 실패 - 코드: " + e.statusCode() + ", 메시지: " + e.getMessage(), e);
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            throw new RuntimeException("AWS SDK 클라이언트 오류: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("파일을 읽는 중 오류가 발생했습니다: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("알 수 없는 다운로드 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * S3에서 파일을 다운로드하고 Content-Type을 함께 반환합니다.
     *
     * @param fileKey S3 버킷 내의 파일 키 (경로)
     * @return 파일 다운로드 정보 (바이트 배열과 Content-Type을 포함)
     */
    public FileDownloadResult downloadFileWithContentType(String fileKey) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("S3 버킷 이름(`aws.s3.bucket-name`)이 설정되어 있지 않습니다.");
        }

        if (fileKey == null || fileKey.isEmpty()) {
            throw new IllegalArgumentException("파일 키가 제공되지 않았습니다.");
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileKey)
                    .build();

            try (ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(getObjectRequest)) {
                byte[] fileBytes = responseInputStream.readAllBytes();
                String contentType = responseInputStream.response().contentType();
                if (contentType == null || contentType.isEmpty()) {
                    // 파일 확장자로 추정
                    if (fileKey.toLowerCase().endsWith(".jpg") || fileKey.toLowerCase().endsWith(".jpeg")) {
                        contentType = "image/jpeg";
                    } else if (fileKey.toLowerCase().endsWith(".png")) {
                        contentType = "image/png";
                    } else {
                        contentType = "application/octet-stream";
                    }
                }
                return new FileDownloadResult(fileBytes, contentType);
            }
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            throw new RuntimeException("S3에서 파일을 찾을 수 없습니다: " + fileKey, e);
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            throw new RuntimeException("S3 다운로드 실패 - 코드: " + e.statusCode() + ", 메시지: " + e.getMessage(), e);
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            throw new RuntimeException("AWS SDK 클라이언트 오류: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("파일을 읽는 중 오류가 발생했습니다: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("알 수 없는 다운로드 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * S3에서 파일의 Content-Type을 가져옵니다.
     *
     * @param fileKey S3 버킷 내의 파일 키 (경로)
     * @return Content-Type 문자열, 없으면 "application/octet-stream"
     */
    public String getContentType(String fileKey) {
        if (bucket == null || bucket.isBlank() || fileKey == null || fileKey.isEmpty()) {
            return "application/octet-stream";
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileKey)
                    .build();

            GetObjectResponse response = s3Client.getObject(getObjectRequest).response();
            String contentType = response.contentType();
            return contentType != null ? contentType : "application/octet-stream";
        } catch (Exception e) {
            // 파일 확장자로 추정
            if (fileKey.toLowerCase().endsWith(".jpg") || fileKey.toLowerCase().endsWith(".jpeg")) {
                return "image/jpeg";
            } else if (fileKey.toLowerCase().endsWith(".png")) {
                return "image/png";
            }
            return "application/octet-stream";
        }
    }

    /**
     * 파일 다운로드 결과를 담는 클래스
     */
    public static class FileDownloadResult {
        private final byte[] fileBytes;
        private final String contentType;

        public FileDownloadResult(byte[] fileBytes, String contentType) {
            this.fileBytes = fileBytes;
            this.contentType = contentType;
        }

        public byte[] getFileBytes() {
            return fileBytes;
        }

        public String getContentType() {
            return contentType;
        }
    }

    /**
     * [보안] 업로드 파일 검증 (Java 시큐어 코딩 가이드 - '위험한 형식 파일 업로드' 방어)
     * 이미지(JPEG, PNG, jpg)와 PDF 파일만 허용합니다.
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        // 파일 크기 제한 (10MB)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("파일 크기가 10MB를 초과할 수 없습니다.");
        }

        // 허용된 content-type 검사 (이미지 jpeg, png)
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        boolean allowed = false;

        if (contentType != null) {
            if ("image/jpeg".equals(contentType) || "image/png".equals(contentType)) {
                allowed = true;
            }
        }

        // content-type이 없거나 신뢰할 수 없는 경우 파일 확장자로 대체 검사
        if (!allowed && filename != null) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")) {
                allowed = true;
            }
        }

        if (!allowed) {
            throw new IllegalArgumentException("지원되지 않는 파일 형식입니다. (JPEG, JPG, PNG만 허용)");
        }
    }

}