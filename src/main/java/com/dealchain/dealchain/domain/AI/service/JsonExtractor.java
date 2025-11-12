package com.dealchain.dealchain.domain.AI.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 임의의 문자열에서 유효한 JSON 블록만 추출하는 유틸리티.
 */
public class JsonExtractor {

    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("\\{.*\\}", Pattern.DOTALL);


    public static String extractJsonBlock(String rawInput) {
        if (rawInput == null || rawInput.trim().isEmpty()) {
            throw new IllegalArgumentException("입력 문자열이 비어있거나 null입니다.");
        }

        Matcher matcher = JSON_BLOCK_PATTERN.matcher(rawInput);

        if (matcher.find()) {
            // matcher.group(0)은 매칭된 전체 문자열 ({...} 포함)을 반환합니다.
            return matcher.group(0).trim();
        }

        // 정규식으로 JSON 블록을 찾지 못한 경우
        throw new IllegalArgumentException("JSON 블록 { ... }이 입력 문자열에서 발견되지 않았습니다. AI 응답 형식을 확인하십시오.");
    }
}