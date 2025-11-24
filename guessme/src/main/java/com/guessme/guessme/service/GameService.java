package com.guessme.guessme.service;

import com.guessme.guessme.config.GeminiConfig;
import com.guessme.guessme.dto.AIResponse;
import com.guessme.guessme.dto.CharacterData;
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

        return Mono.just(new AIResponse(text, false, null));
    }

    public Mono<AIResponse> askAI(String question) {

        conversationHistory += "\nUsuário: " + question;

        String finalPrompt =
                """
                Você está jogando GuessMe.
                Você deve responder SOMENTE com "Sim", "Não" ou "Talvez".

                ❗QUANDO O JOGADOR ACERTAR, siga EXATAMENTE este formato:

                Sim! O personagem é <NOME>.
                Obra: <OBRA>
                Imagem: <URL_DA_IMAGEM_GENERADA>

                REGRAS PARA A IMAGEM:
                - Gere UMA imagem ULTRA-REALISTA usando o modelo Gemini.
                - Proporção obrigatória 3:4.
                - A imagem deve ser fiel ao visual real do personagem.
                - Fundo neutro.
                - Apenas o personagem.
                - Não coloque texto na imagem.
                - Retorne apenas a URL direta da imagem gerada.
                
                Histórico:
                """ + conversationHistory +
                        """

                        Agora responda seguindo as regras:
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
                        ex -> Mono.just(new AIResponse("Erro da API Gemini: " + ex.getResponseBodyAsString(), false, null)))
                .onErrorResume(Exception.class,
                        ex -> Mono.just(new AIResponse("Erro inesperado: " + ex.getMessage(), false, null)));
    }

    private AIResponse extractAIResponse(Map<String, Object> response) {

        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) response.getOrDefault("candidates", List.of());

        if (candidates.isEmpty()) {
            return new AIResponse("Resposta vazia da IA.", false, null);
        }

        Map<String, Object> content =
                (Map<String, Object>) candidates.get(0).getOrDefault("content", Map.of());

        List<Map<String, Object>> parts =
                (List<Map<String, Object>>) content.getOrDefault("parts", List.of());

        String text = parts.isEmpty()
                ? "Resposta inválida da IA."
                : parts.get(0).getOrDefault("text", "").toString().trim();

        conversationHistory += "\nIA: " + text;

        boolean venceu = text.startsWith("Sim! O personagem é");

        if (!venceu) {
            return new AIResponse(text, false, null);
        }

        // Extração limpa
        String nome = extrair(text, "Sim! O personagem é", ".");
        String obra = extrair(text, "Obra:", "\n");
        String imagem = extrair(text, "Imagem:", "\n");

        CharacterData data = new CharacterData(
                nome != null ? nome : "",
                obra != null ? obra : "",
                imagem != null ? imagem : ""
        );

        return new AIResponse(text, true, data);
    }

    private String extrair(String texto, String inicio, String fim) {
        try {
            int i = texto.indexOf(inicio);
            if (i < 0) return null;

            int start = i + inicio.length();
            int j = texto.indexOf(fim, start);

            if (j < 0) j = texto.length();

            return texto.substring(start, j).trim();
        } catch (Exception e) {
            return null;
        }
    }

    public void resetGame() {
        conversationHistory = "";
    }
}
