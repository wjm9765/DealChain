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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;


import java.io.FileOutputStream;//파일 저장을 위해 추가(테스트용)

@Service
public class JsonToPdfService {

    private static final int MAX_JSON_SIZE = 5_242_880;//5MB

    private final XssSanitizer xssSanitizer;
    private final ObjectMapper objectMapper;
    private final S3UploadService s3UploadService;
    private PDType0Font nanumGothicFont;
    private static final Logger log = LoggerFactory.getLogger(JsonToPdfService.class);

    // A4 페이지 크기 (pt)
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float MARGIN_X = 70;
    private static final float MARGIN_TOP = 780; // (페이지 상단 Y 좌표)

    @Autowired
    public JsonToPdfService(XssSanitizer xssSanitizer,
                            ObjectMapper objectMapper,
                            S3UploadService s3UploadService) {
        this.xssSanitizer = xssSanitizer;
        this.objectMapper = objectMapper;
        this.s3UploadService = s3UploadService;
    }

    /**
     * 폰트 로드 (보안: classpath, 폰트명 Font.ttf로 수정)
     */
    @PostConstruct
    public void loadFont() {
        try (InputStream fontStream = new ClassPathResource("fonts/Font.ttf").getInputStream()) { // 👈 폰트명 수정
            try (PDDocument tempDoc = new PDDocument()) {
                this.nanumGothicFont = PDType0Font.load(tempDoc, fontStream);
            }
        } catch (Exception e) {
            log.error("치명적 오류: PDF 한글 폰트(Font.ttf) 로드에 실패했습니다.", e);
            throw new RuntimeException("PDF 한글 폰트 로드 실패", e);
        }
    }


//    //나중에 삭제해야됨,테스트용 함수
//    private void savePdfToDesktopForTesting(byte[] pdfBytes) {
//        try {
//            // [보안] 'resources'가 아닌 '사용자 홈 디렉토리' (예: C:\Users\YourUser 또는 /home/YourUser)
//            String userHome = System.getProperty("user.home");
//            String desktopPath = userHome + "/Desktop"; // 바탕화면 경로
//            String filePath = desktopPath + "/test_contract.pdf";
//
//            log.warn("--- [테스트 전용 보안 경고] ---");
//            log.warn("'java 시큐어 코딩 가이드' (84p) 위반: 민감한 PDF를 서버 디스크에 저장합니다.");
//            log.warn("저장 위치: {}", filePath);
//            log.warn("프로덕션 배포 전 이 'savePdfToDesktopForTesting' 호출 코드를 반드시 제거하십시오.");
//            log.warn("------------------------------");
//
//            // '디스크'에 파일 쓰기 (C++의 fwrite와 유사)
//            try (FileOutputStream fos = new FileOutputStream(filePath)) {
//                fos.write(pdfBytes);
//            }
//
//        } catch (Exception e) {
//            // 테스트용 저장이 실패해도, 메인 로직(S3 업로드)은 중단되면 안 됨.
//            log.error("테스트용 PDF 파일 저장 실패 (메인 로직 계속 진행): {}", e.getMessage());
//        }
//    }

    /**
     * [핵심 수정] JSON과 2개의 'S3 서명 키'로 PDF를 생성하는 메인 함수
     *
     * @param aiContractJson    AI가 생성한 JSON 문자열
     * @param sellerSignatureKey 판매자 서명의 S3 파일 키
     * @param buyerSignatureKey  구매자 서명의 S3 파일 키
     * @return PDF 파일의 byte 배열
     */
    public byte[] createPdf(String aiContractJson,
                            String sellerSignatureKey,
                            String buyerSignatureKey) throws Exception {

        if (aiContractJson == null || aiContractJson.length() > MAX_JSON_SIZE) {
            log.error("DoS 공격 의심: AI JSON 크기가 {}바이트를 초과했습니다. (Size: {})",
                    MAX_JSON_SIZE, (aiContractJson == null ? 0 : aiContractJson.length()));
            throw new IllegalArgumentException("AI가 생성한 계약서 데이터가 너무 큽니다.");
        }


        // --- 1. [보안] XSS 살균 (JSON -> 순수 텍스트 Map) ---
        Map<String, Object> contractMap = sanitizeJsonMap(aiContractJson);

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {

                // --- 3. 텍스트 그리기 (NPE-Safe 헬퍼 사용) ---
                drawText(stream, "자동 생성 계약서 (초안)", (PAGE_WIDTH - 180) / 2, MARGIN_TOP, 20); // (중앙 정렬)

                float currentY = MARGIN_TOP - 60; // 텍스트 시작 Y 좌표

                // [NPE-Safe] 헬퍼를 사용하여 AI JSON 데이터 추출
                Map<String, Object> parties = getMap(contractMap, "parties");
                Map<String, Object> item = getMap(contractMap, "item");
                Map<String, Object> payment = getMap(contractMap, "payment");
                Map<String, Object> deal = getMap(contractMap, "how to deal");
                String specialTerms = getString(contractMap, "specialTerms");

                // 계약 내용 그리기
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


                // --- 4. 서명란 그리기 (페이지 하단) ---

                // 판매자 서명 (왼쪽 하단)
                float sellerSignY = 150;
                String sellerText = "판매자 (갑): " + getString(parties, "sellerName");
                drawText(stream, sellerText, MARGIN_X, sellerSignY, 12);
                drawText(stream, "--------------------", MARGIN_X, sellerSignY + 5, 12);

                //판매자 서명을 "두 줄 중에 위에" 그리기 (좌표: MARGIN_X, sellerSignY + 20)
                drawImageFromS3(document, stream, sellerSignatureKey, MARGIN_X, sellerSignY + 20);

                // 구매자 서명 (오른쪽 하단)
                float buyerSignX = MARGIN_X + 280;
                float buyerSignY = 150;
                String buyerText = "구매자 (을): " + getString(parties, "buyerName");
                drawText(stream, buyerText, buyerSignX, buyerSignY, 12);
                drawText(stream, "--------------------", buyerSignX, buyerSignY + 5, 12);

                // [요청 사항] 구매자 서명을 "두 줄 중에 위에" 그리기 (좌표: buyerSignX, buyerSignY + 20)
                drawImageFromS3(document, stream, buyerSignatureKey, buyerSignX, buyerSignY + 20);

            } // contentStream 닫기

            document.save(out);

            //나중에 삭제해야됨
            //byte[] pdfBytes = out.toByteArray();
            //savePdfToDesktopForTesting(pdfBytes);
            //
            return out.toByteArray();
        } // document 닫기
    }

    /**
     * [신규] S3에서 이미지를 다운로드하여 PDF의 (x, y) 좌표에 그리는 헬퍼 메서드
     */
    private void drawImageFromS3(PDDocument document, PDPageContentStream stream, String s3Key, float x, float y) {
        if (s3Key == null || s3Key.isEmpty()) {
            return; // S3 키가 없으면 아무것도 안 함
        }
        try {
            // [보안] S3UploadService를 통해 '신뢰할 수 있는' 버킷에서 이미지 다운로드
            byte[] imageBytes = s3UploadService.downloadFile(s3Key);
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, imageBytes, s3Key);

            // (x, y) 좌표에 이미지 그리기 (예: 60x30 크기 고정)
            stream.drawImage(pdImage, x, y, 60, 30);

        } catch (Exception e) {
            // [보안] 서명 이미지 다운로드/삽입 실패는 '경고'만 하고 PDF 생성은 계속 (DoS 방지)
            log.warn("S3 서명 이미지 다운로드/삽입 실패 (PDF 생성은 계속됨). Key: {}, Error: {}",
                    s3Key, e.getMessage());
        }
    }


    private Map<String, Object> sanitizeJsonMap(String jsonString) throws Exception {
        TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {};
        Map<String, Object> map = objectMapper.readValue(jsonString, typeRef);
        sanitizeMapRecursively(map);
        return map;
    }

    @SuppressWarnings("unchecked")
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


//    //map 데이터 타입 강제 변환 예외처리
//    private void sanitizeMapRecursively(Map<String, Object> map) {
//        for (Map.Entry<String, Object> entry : map.entrySet()) {
//            Object value = entry.getValue();
//            if (value instanceof String) {
//                entry.setValue(xssSanitizer.sanitizeToPlainText((String) value));
//            } else if (value instanceof Map) {
//                sanitizeMapRecursively((Map<String, Object>) value);
//            }
//        }
//    }

    // --- PDF 텍스트 그리기를 위한 NPE-Safe 헬퍼 메서드들 ---

    /**
     * [NPE-Safe] PDF에 텍스트를 그립니다. (Null-Safe)
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
     * [NPE-Safe] PDF에 한 줄의 텍스트를 그리고 Y 좌표를 업데이트합니다.
     */
    private float drawTextLine(PDPageContentStream stream, String text, float y) throws Exception {
        float fontSize = 11;
        float leading = 16; // 줄 간격
        drawText(stream, text, MARGIN_X, y, fontSize);
        return y - leading;
    }

    /**
     * [NPE-Safe] PDF에 섹션 제목을 그립니다.
     */
    private float drawSection(PDPageContentStream stream, String text, float y) throws Exception {
        float fontSize = 14;
        float leading = 20;
        drawText(stream, text, MARGIN_X, y, fontSize);
        return y - leading;
    }

    /**
     * [NPE-Safe] Map에서 값을 String으로 안전하게 꺼냅니다.
     */
    private String getString(Map<String, Object> map, String key) {
        if (map == null) return "(정보 없음)";
        Object val = map.get(key);
        return (val == null) ? "(정보 없음)" : String.valueOf(val);
    }

    /**
     * [NPE-Safe] Map에서 중첩된 Map을 안전하게 꺼냅니다.
     */
    private Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Map) {
            // Jackson이 숫자를 Integer/Long/Double 등으로 파싱하므로,
            // Map<String, Object>로 안전하게 캐스팅합니다.
            @SuppressWarnings("unchecked")
            Map<String, Object> nestedMap = (Map<String, Object>) val;
            return nestedMap;
        }
        return Collections.emptyMap();
    }
}