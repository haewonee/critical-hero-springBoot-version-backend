package com.devassistant.critical_hero_springboot_version.global.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenAiClient {

    private final WebClient embeddingClient;
    private final WebClient chatClient;
    private final String embeddingModel;
    private final String chatModel;

    public OpenAiClient(
            @Value("${openai.embedding-url}") String embeddingUrl,
            @Value("${openai.chat-url}") String chatUrl,
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.embedding-model}") String embeddingModel,
            @Value("${openai.chat-model}") String chatModel
    ) {
        this.embeddingModel = embeddingModel;
        this.chatModel = chatModel;

        WebClient base = WebClient.builder()
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();

        this.embeddingClient = base.mutate().baseUrl(embeddingUrl).build();
        this.chatClient = base.mutate().baseUrl(chatUrl).build();
    }

    // 텍스트 → 임베딩 벡터 생성
    public float[] createEmbedding(String text) {
        Map<String, Object> requestBody = Map.of(
                "model", embeddingModel,
                "input", text
        );

        Map response = embeddingClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        List<Double> embeddingList = (List<Double>) data.get(0).get("embedding");

        float[] embedding = new float[embeddingList.size()];
        for (int i = 0; i < embeddingList.size(); i++) {
            embedding[i] = embeddingList.get(i).floatValue();
        }
        return embedding;
    }

    // float[] → pgvector INSERT용 문자열 "[0.1, 0.2, ...]"
    public String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            sb.append(embedding[i]);
            if (i < embedding.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    // 커밋 컨텍스트 + 질문 → GPT 분석 결과
    public String analyze(String systemPrompt, String userMessage) {
        Map<String, Object> requestBody = Map.of(
                "model", chatModel,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        Map response = chatClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}
