package com.dealchain.dealchain.domain.AI;


import com.dealchain.dealchain.domain.AI.dto.detectDto;
import com.dealchain.dealchain.domain.AI.service.ApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TestController {

    private final ApiService flaskApiService;

    /**
     * API Gateway 호출을 테스트하기 위한 GET 컨트롤러
     * 호출 방법: (GET) http://localhost:8080/test
     */
    @GetMapping("/test")
    public void testApiGatewayEndpoint() {
        log.info("GET /test 요청 수신. API Gateway 실제 호출을 테스트합니다.");

        final String hardcodedChatLog = "{\n  \"chat_history\": [\n    {\"id\": 1, \"message\": \"안녕하세요! 저는 여러분이 원하는 제품을 팔고 있어요.\"},\n    {\"id\": 2, \"message\": \"오, 안녕하세요. 어떤 제품을 팔고 계시죠?\"},\n    {\"id\": 1, \"message\": \"제가 판매하는 제품은 최신 스마트폰입니다. 사진을 보내드릴게요.\"},\n    {\"id\": 2, \"message\": \"와, 정말 좋은 것 같네요. 가격은 어떻게 되나요?\"},\n    {\"id\": 1, \"message\": \"가격은 50만 원입니다. 다른 곳에서는 60만 원 이상 하는 제품이에요.\"},\n    {\"id\": 2, \"message\": \"좋네요. 그런데 어떻게 결제하죠?\"},\n    {\"id\": 1, \"message\": \"저는 계좌이체만 받습니다. 선입금 후 택배로 보내드릴게요.\"},\n    {\"id\": 2, \"message\": \"알겠습니다. 계좌번호를 알려주세요.\"},\n    {\"id\": 1, \"message\": \"계좌번호는 123-456-7890123 입니다. 입금 후 주소를 남겨주세요.\"},\n    {\"id\": 2, \"message\": \"네, 입금하고 주소 남기겠습니다.\"},\n    {\"id\": 1, \"message\": \"좋습니다. 입금 확인 후 바로 택배로 보내드리겠습니다. 배송은 2-3일 정도 걸릴 예정이에요.\"},\n    {\"id\": 2, \"message\": \"입금 완료했습니다! 주소는 서울시 강남구 역삼동 123-45입니다.\"},\n    {\"id\": 1, \"message\": \"입금 확인 후 바로 보내드릴게요. 조금만 기다려주세요.\"},\n    {\"id\": 2, \"message\": \"기다리고 있겠습니다. 혹시 배송 추적은 어떻게 하나요?\"},\n    {\"id\": 1, \"message\": \"배송 추적은 제가 보내드린 후에 알려드리겠습니다. 괜찮으시죠?\"},\n    {\"id\": 2, \"message\": \"네, 감사합니다! 기다릴게요.\"},\n    {\"id\": 1, \"message\": \"근데 이 제품은 한정 판매라서 빠르게 처리해야 합니다. 입금 확인되면 바로 택배로 보내드릴게요.\"},\n    {\"id\": 2, \"message\": \"알겠습니다. 정말 감사합니다!\"}\n  ]\n}\n";

        try {
            detectDto dto = flaskApiService.sendPostRequest(hardcodedChatLog);

            System.out.println("--- 🚀 API Gateway 실제 응답 🚀 ---");
            System.out.println("fraud_score: " + Objects.toString(dto.getFraud_score(), "(null)"));
            System.out.println("fraud_type : " + Objects.toString(dto.getFraud_type(), "(null)"));
            System.out.println("message_id : " + Objects.toString(dto.getMessage_id(), "(null)"));
            System.out.println("reason     : " + Objects.toString(dto.getReason(), "(null)"));
            System.out.println("-----------------------------------");

        } catch (Exception e) {
            log.error("API Gateway /test 엔드포인트 호출 중 오류 발생", e);
            e.printStackTrace();
        }
    }
}