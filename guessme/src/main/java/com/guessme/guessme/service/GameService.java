package com.guessme.guessme.service;

import com.guessme.guessme.dto.AIResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
public class GameService {

    private final WebClient webClient;

    public GameService(@Value("${openai.api.key}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1/chat/completions")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public Mono<AIResponse> askAI(String question) {
        String requestBody = """
            {
                "model": "gpt-4o-mini",
                "messages": [
                    {"role": "system", "content": "Você é o assistente do jogo GuessMe. Responda de forma curta, divertida e natural."},
                    {"role": "user", "content": "%s"}
                ]
            }
        """.formatted(question);

        return webClient.post()
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(String.class)
                .map(AIResponse::new)
                .onErrorResume(WebClientResponseException.class, ex ->
                        Mono.just(new AIResponse("Erro da API OpenAI: " + ex.getResponseBodyAsString()))
                )
                .onErrorResume(Exception.class, ex ->
                        Mono.just(new AIResponse("Erro inesperado: " + ex.getMessage()))
                );
    }
}
