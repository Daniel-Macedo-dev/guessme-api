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

    private String conversationHistory = "";

    public Mono<AIResponse> startGame() {
        conversationHistory = "";

        String text = "Ok! Já escolhi um personagem. Pode fazer sua primeira pergunta!";
        conversationHistory += "\nIA: " + text;

        return Mono.just(new AIResponse(text, false));
    }

    public Mono<AIResponse> askAI(String question) {

        conversationHistory += "\nUsuário: " + question;

        String finalPrompt =
                """
                Você está participando de um jogo de adivinhação.
                Você deve responder SOMENTE com "Sim", "Não" ou "Talvez" até que o jogador acerte o personagem.

                ⚠️ QUANDO O JOGADOR ACERTAR, SIGA EXATAMENTE ESTE FORMATO:

                CORRECT_GUESS: Sim! O personagem é <NOME>.
                (NADA antes, nada depois, nada além disso.)

                Se não for acerto, responda APENAS com "Sim", "Não" ou "Talvez".

                Historico atual:
                """ + conversationHistory +
                        """
        
                        Agora responda corretamente:
                        """;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(Map.of("text", finalPrompt))
                        )
                )
        );

        String url = "/models/gemini-2.5-flash:generateContent?key=" + geminiConfig.getGeminiApiKey();

        return geminiWebClient.post()
                .uri(url)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::extractAIResponse)
                .onErrorResume(WebClientResponseException.class,
                        ex -> Mono.just(new AIResponse("Erro da API Gemini: " + ex.getResponseBodyAsString(), false)))
                .onErrorResume(Exception.class,
                        ex -> Mono.just(new AIResponse("Erro inesperado: " + ex.getMessage(), false)));
    }

    private AIResponse extractAIResponse(Map<String, Object> response) {

        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) response.getOrDefault("candidates", List.of());

        if (candidates.isEmpty()) {
            return new AIResponse("Resposta vazia da IA.", false);
        }

        Map<String, Object> content =
                (Map<String, Object>) candidates.get(0).getOrDefault("content", Map.of());

        List<Map<String, Object>> parts =
                (List<Map<String, Object>>) content.getOrDefault("parts", List.of());

        String text = parts.isEmpty()
                ? "Resposta inválida da IA."
                : parts.get(0).getOrDefault("text", "").toString().trim();

        conversationHistory += "\nIA: " + text;

        boolean venceu = text
                .toLowerCase()
                .replace(" ", "")
                .startsWith("correct_guess:".replace(" ", ""));

        return new AIResponse(text, venceu);
    }

    public void resetGame() {
        conversationHistory = "";
    }
}
