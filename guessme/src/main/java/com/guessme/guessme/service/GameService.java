package com.guessme.guessme.service;

import com.guessme.guessme.dto.AIResponse;
import com.guessme.guessme.config.GeminiConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
public class GameService {

    private final WebClient webClient;
    private final GeminiConfig geminiConfig;

    public GameService(WebClient geminiWebClient, GeminiConfig geminiConfig) {
        this.webClient = geminiWebClient;
        this.geminiConfig = geminiConfig;
    }

    public Mono<AIResponse> askAI(String question) {
        String requestBody = """
            {
              "contents": [{
                "role": "user",
                "parts": [{"text": "%s"}]
              }]
            }
        """.formatted(question.replace("\"", "\\\""));

        String url = "/gemini-1.5-flash:generateContent?key=" + geminiConfig.getGeminiApiKey();

        return webClient.post()
                .uri(url)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(String.class)
                .map(AIResponse::new)
                .onErrorResume(WebClientResponseException.class, ex ->
                        Mono.just(new AIResponse("Erro da API Gemini: " + ex.getResponseBodyAsString()))
                )
                .onErrorResume(Exception.class, ex ->
                        Mono.just(new AIResponse("Erro inesperado: " + ex.getMessage()))
                );
    }
}
