package com.guessme.guessme.service;

import com.guessme.guessme.config.GeminiConfig;
import com.guessme.guessme.dto.AIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GeminiConfig geminiConfig;

    private final WebClient geminiWebClient;

    public Mono<AIResponse> askAI(String question) {

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", question)
                                )
                        )
                )
        );

        String url = "/models/gemini-2.5-flash:generateContent?key=" + geminiConfig.getGeminiApiKey();

        return geminiWebClient.post()
                .uri(url)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    try {
                        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                        String text = parts.get(0).get("text").toString();

                        return new AIResponse(text);
                    } catch (Exception e) {
                        return new AIResponse("Erro ao processar resposta da IA.");
                    }
                })
                .onErrorResume(WebClientResponseException.class, ex ->
                        Mono.just(new AIResponse("Erro da API Gemini: " + ex.getResponseBodyAsString()))
                )
                .onErrorResume(Exception.class, ex ->
                        Mono.just(new AIResponse("Erro inesperado: " + ex.getMessage()))
                );
    }
}
