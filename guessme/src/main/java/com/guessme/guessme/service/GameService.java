package com.guessme.guessme.service;

import com.guessme.guessme.dto.AIResponse;
import com.guessme.guessme.config.GeminiConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GeminiConfig geminiConfig;
    private final WebClient geminiWebClient;

    public Mono<AIResponse> askAI(String question) {

        String model = "gemini-2.5-flash";
        String url = "/models/" + model + ":generateContent?key=" + geminiConfig.getGeminiApiKey();
        String requestBody = """
            {
              "contents": [{
                "parts": [{
                  "text": "%s"
                }]
              }]
            }
        """.formatted(question.replace("\"", "\\\""));

        return geminiWebClient.post()
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
