package com.dealchain.dealchain.domain.contract.service;

import com.dealchain.dealchain.domain.security.S3UploadService;
import com.dealchain.dealchain.domain.security.XssSanitizer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import java.io.FileOutputStream;

@Service
public class JsonToPdfService {

    private final XssSanitizer xssSanitizer;
    private final ObjectMapper objectMapper;
    private final S3UploadService s3UploadService;
    private PDType0Font nanumGothicFont;
    private static final Logger log = LoggerFactory.getLogger(JsonToPdfService.class);

    // A4 페이지 크기 (pt)
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float MARGIN_X = 70;
    private static final float MARGIN_TOP = 780; // (페이지 상단 Y 좌표)

    public JsonToPdfService(XssSanitizer xssSanitizer,
                            ObjectMapper objectMapper,
                            S3UploadService s3UploadService) {
        this.xssSanitizer = xssSanitizer;
        this.objectMapper = objectMapper;
        this.s3UploadService = s3UploadService;
    }

    /**
     * 폰트 로드
     */
    @PostConstruct
    public void loadFont() {
        try (InputStream fontStream = new ClassPathResource("fonts/Font.ttf").getInputStream()) {
            try (PDDocument tempDoc = new PDDocument()) {
                this.nanumGothicFont = PDType0Font.load(tempDoc, fontStream);
            }
        } catch (Exception e) {
            log.error("치명적 오류: PDF 한글 폰트(Font.ttf) 로드에 실패했습니다.", e);
            throw new RuntimeException("PDF 한글 폰트 로드 실패", e);
        }
    }

    /**
     * 테스트용: PDF를 바탕화면에 저장
     * 프로덕션 배포 전 제거 필요
     */
    private void savePdfToDesktopForTesting(byte[] pdfBytes) {
        try {
            String userHome = System.getProperty("user.home");
            String desktopPath = userHome + "/Desktop";
            String filePath = desktopPath + "/test_contract.pdf";

            log.warn("--- [테스트 전용 보안 경고] ---");
            log.warn("'java 시큐어 코딩 가이드' (84p) 위반: 민감한 PDF를 서버 디스크에 저장합니다.");
            log.warn("저장 위치: {}", filePath);
            log.warn("프로덕션 배포 전 이 'savePdfToDesktopForTesting' 호출 코드를 반드시 제거하십시오.");
            log.warn("------------------------------");
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(pdfBytes);
            }

        } catch (Exception e) {
            log.error("테스트용 PDF 파일 저장 실패 (메인 로직 계속 진행): {}", e.getMessage());
        }
    }

    /**
     * JSON과 2개의 S3 서명 키로 PDF를 생성
     *
     * @param aiContractJson    AI가 생성한 JSON 문자열
     * @param sellerSignatureKey 판매자 서명의 S3 파일 키
     * @param buyerSignatureKey  구매자 서명의 S3 파일 키
     * @return PDF 파일의 byte 배열
     */
    public byte[] createPdf(String aiContractJson,
                            String sellerSignatureKey,
                            String buyerSignatureKey) throws Exception {

        // XSS 방지: JSON 데이터 재귀적 살균
        Map<String, Object> contractMap = sanitizeJsonMap(aiContractJson);

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {

                drawText(stream, "자동 생성 계약서 (초안)", (PAGE_WIDTH - 180) / 2, MARGIN_TOP, 20);

                float currentY = MARGIN_TOP - 60;
                
                // AI JSON 데이터 추출 (Null-Safe)
                Map<String, Object> parties = getMap(contractMap, "parties");
                Map<String, Object> item = getMap(contractMap, "item");
                Map<String, Object> payment = getMap(contractMap, "payment");
                Map<String, Object> deal = getMap(contractMap, "how to deal");
                String specialTerms = getString(contractMap, "specialTerms");
                currentY = drawSection(stream, "1. 거래 당사자", currentY);
                currentY = drawTextLine(stream, " - 판매자 (갑): " + getString(parties, "sellerName"), currentY);
                currentY = drawTextLine(stream, " - 구매자 (을): " + getString(parties, "buyerName"), currentY);

                currentY = drawSection(stream, "2. 거래 물품", currentY - 10);
                currentY = drawTextLine(stream, " - 물품명: " + getString(item, "name"), currentY);
                currentY = drawTextLine(stream, " - 물품상태: " + getString(item, "condition"), currentY);

                currentY = drawSection(stream, "3. 거래 대금", currentY - 10);
                currentY = drawTextLine(stream, " - 가격: " + getString(payment, "price") + " 원", currentY);
                currentY = drawTextLine(stream, " - 지급방식: " + getString(payment, "method"), currentY);

                currentY = drawSection(stream, "4. 거래 방법", currentY - 10);
                currentY = drawTextLine(stream, " - 방식: " + getString(deal, "method"), currentY);
                currentY = drawTextLine(stream, " - 시간: " + getString(deal, "dateTime"), currentY);
                currentY = drawTextLine(stream, " - 장소: " + getString(deal, "location"), currentY);

                currentY = drawSection(stream, "5. 특약 사항", currentY - 10);
                currentY = drawTextLine(stream, " - " + specialTerms, currentY);

                // 판매자 서명 (왼쪽 하단)
                float sellerSignY = 150;
                String sellerText = "판매자 (갑): " + getString(parties, "sellerName");
                drawText(stream, sellerText, MARGIN_X, sellerSignY, 12);
                drawText(stream, "--------------------", MARGIN_X, sellerSignY + 5, 12);
                drawImageFromS3(document, stream, sellerSignatureKey, MARGIN_X, sellerSignY + 20);

                // 구매자 서명 (오른쪽 하단)
                float buyerSignX = MARGIN_X + 280;
                float buyerSignY = 150;
                String buyerText = "구매자 (을): " + getString(parties, "buyerName");
                drawText(stream, buyerText, buyerSignX, buyerSignY, 12);
                drawText(stream, "--------------------", buyerSignX, buyerSignY + 5, 12);
                drawImageFromS3(document, stream, buyerSignatureKey, buyerSignX, buyerSignY + 20);

            }

            document.save(out);

            byte[] pdfBytes = out.toByteArray();
            // 테스트용: PDF를 바탕화면에 저장 (프로덕션 배포 전 제거 필요)
            savePdfToDesktopForTesting(pdfBytes);

            return out.toByteArray();
        }
    }

    /**
     * S3에서 이미지를 다운로드하여 PDF의 (x, y) 좌표에 그리기
     */
    private void drawImageFromS3(PDDocument document, PDPageContentStream stream, String s3Key, float x, float y) {
        if (s3Key == null || s3Key.isEmpty()) {
            return;
        }
        try {
            // S3UploadService를 통해 신뢰할 수 있는 버킷에서 이미지 다운로드
            byte[] imageBytes = s3UploadService.downloadFile(s3Key);
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, imageBytes, s3Key);
            stream.drawImage(pdImage, x, y, 60, 30);

        } catch (Exception e) {
            // 서명 이미지 실패해도 PDF 생성 계속 (DoS 방지)
            log.warn("S3 서명 이미지 다운로드/삽입 실패 (PDF 생성은 계속됨). Key: {}, Error: {}",
                    s3Key, e.getMessage());
        }
    }

    /**
     * JSON 문자열을 Map으로 변환하고 XSS 공격 방지를 위해 재귀적으로 살균
     */
    private Map<String, Object> sanitizeJsonMap(String jsonString) throws Exception {
        TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {};
        Map<String, Object> map = objectMapper.readValue(jsonString, typeRef);
        sanitizeMapRecursively(map);
        return map;
    }

    /**
     * Map의 모든 String 값을 재귀적으로 XSS 살균
     */
    private void sanitizeMapRecursively(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                entry.setValue(xssSanitizer.sanitizeToPlainText((String) value));
            } else if (value instanceof Map) {
                sanitizeMapRecursively((Map<String, Object>) value);
            }
        }
    }

    /**
     * PDF에 텍스트를 그리기
     */
    private void drawText(PDPageContentStream stream, String text, float x, float y, float fontSize) throws Exception {
        if (text == null) {
            text = "(정보 없음)";
        }
        stream.beginText();
        stream.setFont(this.nanumGothicFont, fontSize);
        stream.newLineAtOffset(x, y);
        stream.showText(text);
        stream.endText();
    }

    /**
     * PDF에 한 줄의 텍스트를 그리고 Y 좌표를 업데이트
     */
    private float drawTextLine(PDPageContentStream stream, String text, float y) throws Exception {
        float fontSize = 11;
        float leading = 16; // 줄 간격
        drawText(stream, text, MARGIN_X, y, fontSize);
        return y - leading;
    }

    /**
     * PDF에 섹션 제목을 그리기
     */
    private float drawSection(PDPageContentStream stream, String text, float y) throws Exception {
        float fontSize = 14;
        float leading = 20;
        drawText(stream, text, MARGIN_X, y, fontSize);
        return y - leading;
    }

    /**
     * Map에서 값을 String으로 안전하게 추출 (Null-Safe)
     * null이면 "(정보 없음)" 반환
     */
    private String getString(Map<String, Object> map, String key) {
        if (map == null) return "(정보 없음)";
        Object val = map.get(key);
        return (val == null) ? "(정보 없음)" : String.valueOf(val);
    }

    /**
     * Map에서 중첩된 Map을 안전하게 추출 (Null-Safe)
     * Map이 아니거나 null이면 빈 Map 반환
     */
    private Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> nestedMap = (Map<String, Object>) val;
            return nestedMap;
        }
        return Collections.emptyMap();
    }
}