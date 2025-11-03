package com.dealchain.dealchain.domain.AI.service;

import com.dealchain.dealchain.domain.AI.dto.ContractDefaultReqeustDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dealchain.dealchain.domain.product.Product;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


@Service
public class ContractDtoJsonConverter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String toJson(ContractDefaultReqeustDto dto) {
        String productStr = buildProductString(dto);
        Map<String, Object> out = new HashMap<>();
        out.put("sellerId", dto.getSellerId());
        out.put("buyerId", dto.getBuyerId());
        out.put("product", productStr);
        try {
            return MAPPER.writeValueAsString(out);
        } catch (Exception e) {
            throw new RuntimeException("JSON 변환 실패", e);
        }
    }

    private static String buildProductString(ContractDefaultReqeustDto dto) {
        Product p = dto.getProduct();
        StringBuilder sb = new StringBuilder();
        sb.append("<product>\n");
        sb.append("\"sellerId\":").append(dto.getSellerId()).append("\n");
        sb.append("\"buyerId\":").append(dto.getBuyerId()).append("\n");
        if (p != null) {
            if (p.getId() != null) sb.append("\"id\":").append(p.getId()).append("\n");
            appendQuoted(sb, "productName", p.getProductName());
            appendQuoted(sb, "title", p.getTitle());
            if (p.getPrice() != null) sb.append("\"price\":").append(p.getPrice()).append("\n");
            appendQuoted(sb, "description", p.getDescription());
            if (p.getMemberId() != null) sb.append("\"memberId\":").append(p.getMemberId()).append("\n");
            // productImage 제외
        }
        sb.append("</product>");
        return sb.toString();
    }

    private static void appendQuoted(StringBuilder sb, String key, String value) {
        if (value != null) {
            sb.append("\"").append(key).append("\":\"").append(escape(value)).append("\"\n");
        }
    }

    private static String escape(String s) {
        return s.replace("\"", "\\\"");
    }
}
