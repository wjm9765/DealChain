// java
package com.dealchain.dealchain.domain.AI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.dealchain.dealchain.domain.AI.service.AICreateContract;

@RestController
public class testcontroller {

    private final AICreateContract aiCreateContract;

    public testcontroller(AICreateContract aiCreateContract) {
        this.aiCreateContract = aiCreateContract;
    }

    @GetMapping("/api/ai/test")
    public ResponseEntity<String> invokeAI(@RequestParam(value = "input", required = false) String input) {
        try {
            String payload = input == null ? "" : input;
            String result = aiCreateContract.invokeClaude(payload);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
