package com.dealchain.dealchain.domain.AI;

import com.dealchain.dealchain.domain.AI.service.SageMakerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j // 로그 사용 (선택 사항)
@RestController
@RequiredArgsConstructor // SageMakerService를 생성자 주입 받습니다.
public class TestController {

    // 실제 SageMakerService 빈을 주입받습니다.
    private final SageMakerService sageMakerService;

    /**
     * SageMaker 엔드포인트 호출을 테스트하기 위한 GET 컨트롤러
     * 호출 방법: (GET) http://localhost:8080/test
     */
    @GetMapping("/test")
    public void testSageMakerEndpoint() {
        log.info("GET /test 요청 수신. SageMaker 실제 호출을 테스트합니다.");

        // [요청사항] 하드코딩된 입력 JSON (Java 15+ Text Block)
        final String hardcodedChatLog = """
                {
                  "chat_history": [
                    {"id": 1, "message": "안녕하세요! 저는 여러분이 원하는 제품을 팔고 있어요."},
                    {"id": 2, "message": "오, 안녕하세요. 어떤 제품을 팔고 계시죠?"},
                    {"id": 1, "message": "제가 판매하는 제품은 최신 스마트폰입니다. 사진을 보내드릴게요."},
                    {"id": 2, "message": "와, 정말 좋은 것 같네요. 가격은 어떻게 되나요?"},
                    {"id": 1, "message": "가격은 50만 원입니다. 다른 곳에서는 60만 원 이상 하는 제품이에요."},
                    {"id": 2, "message": "좋네요. 그런데 어떻게 결제하죠?"},
                    {"id": 1, "message": "저는 계좌이체만 받습니다. 선입금 후 택배로 보내드릴게요."},
                    {"id": 2, "message": "알겠습니다. 계좌번호를 알려주세요."},
                    {"id": 1, "message": "계좌번호는 123-456-7890123 입니다. 입금 후 주소를 남겨주세요."},
                    {"id": 2, "message": "네, 입금하고 주소 남기겠습니다."},
                    {"id": 1, "message": "좋습니다. 입금 확인 후 바로 택배로 보내드리겠습니다. 배송은 2-3일 정도 걸릴 예정이에요."},
                    {"id": 2, "message": "입금 완료했습니다! 주소는 서울시 강남구 역삼동 123-45입니다."},
                    {"id": 1, "message": "입금 확인 후 바로 보내드릴게요. 조금만 기다려주세요."},
                    {"id": 2, "message": "기다리고 있겠습니다. 혹시 배송 추적은 어떻게 하나요?"},
                    {"id": 1, "message": "배송 추적은 제가 보내드린 후에 알려드리겠습니다. 괜찮으시죠?"},
                    {"id": 2, "message": "네, 감사합니다! 기다릴게요."},
                    {"id": 1, "message": "근데 이 제품은 한정 판매라서 빠르게 처리해야 합니다. 입금 확인되면 바로 택배로 보내드릴게요."},
                    {"id": 2, "message": "알겠습니다. 정말 감사합니다!"}
                  ]
                }\
                """;

        try {
            // [요청사항] 실제 SageMakerService 호출
            String aiResponse = sageMakerService.invokeEndpoint(hardcodedChatLog);

            // [요청사항] sout으로 결과 출력
            System.out.println("--- 🚀 SageMaker 실제 응답 🚀 ---");
            System.out.println(aiResponse);
            System.out.println("-----------------------------------");

        } catch (Exception e) {
            log.error("SageMaker /test 엔드포인트 호출 중 오류 발생", e);
            // 오류 발생 시 콘솔에 스택 트레이스 출력
            e.printStackTrace();
        }
    }
}
