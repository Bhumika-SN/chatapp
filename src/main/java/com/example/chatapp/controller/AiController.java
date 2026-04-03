package com.example.chatapp.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/ai")
public class AiController {

    @Value("${groq.api.key}")
    private String apiKey;

    @PostMapping("/summarize")
    public Map<String, Object> summarize(@RequestBody Map<String, String> body) {
        String messages = body.get("messages");
        Map<String, Object> result = new HashMap<>();

        try {
            RestTemplate rest = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", "Summarize this chat in 3 bullet points. Be concise:\n\n" + messages);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama-3.3-70b-versatile");
            requestBody.put("max_tokens", 400);
            requestBody.put("messages", List.of(userMsg));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = rest.postForEntity(
                    "https://api.groq.com/openai/v1/chat/completions",
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String summary = message.get("content").toString();

            result.put("summary", summary);

        } catch (Exception e) {
            result.put("error", e.getMessage());
        }

        return result;
    }
}