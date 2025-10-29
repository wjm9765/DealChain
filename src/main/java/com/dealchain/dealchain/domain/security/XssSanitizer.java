package com.dealchain.dealchain.domain.security;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class XssSanitizer {

    // 1. 중고거래 채팅용 정책 (간결)
    private final PolicyFactory chatPolicy;

    // 2. 중고거래 게시글용 정책 (더 풍부한 서식 허용)
    private final PolicyFactory postPolicy;

    public XssSanitizer() {
        Pattern httpHttpsPattern = Pattern.compile("^https?://.*");

        // --- 채팅용 정책 정의 ---
        this.chatPolicy = new HtmlPolicyBuilder()
                .allowStandardUrlProtocols()
                // 링크 (a): href 속성만, nofollow 추가
                .allowElements("a")
                .requireRelNofollowOnLinks()
                .allowAttributes("href").onElements("a")
                // 이미지 (img): src(http/https), alt, width, height 허용
                .allowElements("img")
                .allowAttributes("src").matching(httpHttpsPattern).onElements("img")
                .allowAttributes("alt", "width", "height").onElements("img")
                // 단락, 줄바꿈, 인용
                .allowElements("p", "br", "blockquote")
                // 그 외 모든 태그 제거 (b, i, u, ul, ol 등 제외)
                .toFactory();

        // --- 게시글용 정책 정의 ---
        this.postPolicy = new HtmlPolicyBuilder()
                .allowStandardUrlProtocols()
                // 링크 (a): href 속성만, nofollow 추가
                .allowElements("a")
                .requireRelNofollowOnLinks()
                .allowAttributes("href").onElements("a")
                // 이미지 (img): src(http/https), alt, width, height 허용
                .allowElements("img")
                .allowAttributes("src").matching(httpHttpsPattern).onElements("img")
                .allowAttributes("alt", "width", "height").onElements("img")
                // 기본적인 텍스트 서식 (채팅과 달리 허용)
                .allowElements("b", "strong", "i", "em", "u", "strike", "s")
                // 목록 (채팅과 달리 허용)
                .allowElements("ul", "ol", "li")
                // 단락, 줄바꿈, 인용
                .allowElements("p", "br", "blockquote")
                // 제목 태그 (게시글 구조화)
                .allowElements("h1", "h2", "h3", "h4", "h5", "h6")
                // 수평선 (구분선)
                .allowElements("hr")
                // 그 외 모든 태그 제거
                .toFactory();
    }

    public String sanitizeForChat(String potentiallyDirtyInput) {//채팅용 검증 함수
        if (potentiallyDirtyInput == null || potentiallyDirtyInput.isEmpty()) {
            return potentiallyDirtyInput;
        }
        return chatPolicy.sanitize(potentiallyDirtyInput);
    }

    public String sanitizeForPost(String potentiallyDirtyInput) {//게시글용 검증 함수
        if (potentiallyDirtyInput == null || potentiallyDirtyInput.isEmpty()) {
            return potentiallyDirtyInput;
        }
        return postPolicy.sanitize(potentiallyDirtyInput);
    }

    public String sanitizeToPlainText(String potentiallyDirtyInput) {//그냥 순수한 텍스트만 남김
        if (potentiallyDirtyInput == null || potentiallyDirtyInput.isEmpty()) {
            return potentiallyDirtyInput;
        }
        PolicyFactory plainTextPolicy = new org.owasp.html.HtmlPolicyBuilder().toFactory();
        return plainTextPolicy.sanitize(potentiallyDirtyInput);
    }
}
