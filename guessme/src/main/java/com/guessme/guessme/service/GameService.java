package com.guessme.guessme.service;

import com.guessme.guessme.config.GeminiConfig;
import com.guessme.guessme.dto.AIResponse;
import com.guessme.guessme.dto.CharacterData;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GameService {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final GeminiConfig geminiConfig;
    private final WebClient geminiWebClient;
    private final ImageSearchService imageSearchService;

    private String conversationHistory = "";
    private String currentCategory = "Geral";

    // ✅ categorias sugeridas
    public List<String> getCategories() {
        return List.of("Geral", "Anime", "Games", "Filmes", "Séries", "Quadrinhos");
    }

    // ✅ Mantém seu startGame antigo (compatível)
    public Mono<AIResponse> startGame() {
        return startGame(null);
    }

    // ✅ Start com categoria (novo)
    public Mono<AIResponse> startGame(String category) {
        conversationHistory = "";

        currentCategory = (category == null || category.isBlank())
                ? "Geral"
                : category.trim();

        String text = "Ok! Já escolhi um personagem"
                + (currentCategory.equalsIgnoreCase("Geral") ? "" : " da categoria: " + currentCategory)
                + ". Pode fazer sua primeira pergunta!";

        conversationHistory += "\n[SISTEMA] Categoria: " + currentCategory;
        conversationHistory += "\nIA: " + text;

        return Mono.just(new AIResponse(text, false, null));
    }

    public Mono<AIResponse> askAI(String question) {
        String key = geminiConfig.getGeminiApiKey();
        if (key == null || key.isBlank()) {
            return Mono.just(new AIResponse(
                    "Config inválida: gemini.api.key não foi carregada (gemini.properties).",
                    false,
                    null
            ));
        }

        conversationHistory += "\nUsuário: " + question;

        String categoryRule = """
                Regras de universo/categoria:
                - Categoria atual: %s
                - Você deve manter UM personagem secreto dentro dessa categoria (ou equivalente).
                - Não revele nome/obra diretamente, a não ser que o jogador acerte.
                """.formatted(currentCategory);

        String finalPrompt =
                """
                Você está jogando GuessMe.

                %s

                Responda APENAS: "Sim", "Não" ou "Talvez".
                Seja consistente com o personagem secreto.

                ❗SE O JOGADOR ACERTAR, responda EXATAMENTE assim (sem texto extra):

                Sim! O personagem é <NOME>.
                Obra: <OBRA>

                Histórico:
                """.formatted(categoryRule)
                        + conversationHistory +
                        "\nAgora responda seguindo as regras.";

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", finalPrompt)
                        ))
                )
        );

        String url = "/models/gemini-2.5-flash:generateContent";

        return geminiWebClient.post()
                .uri(url)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(MAP_TYPE)
                .flatMap(this::extractAIResponseReactive)
                .onErrorResume(WebClientResponseException.class,
                        (WebClientResponseException ex) -> {
                            String details = ex.getResponseBodyAsString();
                            if (details == null || details.isBlank()) {
                                details = ex.getMessage();
                            }
                            return Mono.just(new AIResponse(
                                    "Erro da API Gemini (" + ex.getStatusCode().value() + "): " + details,
                                    false,
                                    null
                            ));
                        })
                .onErrorResume(Throwable.class, (Throwable ex) ->
                        Mono.just(new AIResponse("Erro inesperado: " + ex.getMessage(), false, null))
                );
    }

    // ✅ NOVO: Hint
    public Mono<AIResponse> hint() {
        String key = geminiConfig.getGeminiApiKey();
        if (key == null || key.isBlank()) {
            return Mono.just(new AIResponse(
                    "Config inválida: gemini.api.key não foi carregada (gemini.properties).",
                    false,
                    null
            ));
        }

        String prompt =
                """
                Você está jogando GuessMe.
                Categoria atual: %s

                Gere UMA dica curta (1 frase) para ajudar o jogador.
                Regras:
                - Não revele o nome do personagem.
                - Não revele o nome da obra.
                - Não diga algo inútil tipo "é famoso".
                - A dica precisa ser útil sem entregar.

                Histórico:
                """.formatted(currentCategory)
                        + conversationHistory
                        + "\nResponda apenas com a dica (sem prefixos).";

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        String url = "/models/gemini-2.5-flash:generateContent";

        return geminiWebClient.post()
                .uri(url)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(MAP_TYPE)
                .flatMap(this::extractTextOnlyReactive)
                .map(hintText -> {
                    // registra no histórico (opcional, mas ajuda consistência)
                    conversationHistory += "\nIA (DICA): " + hintText;
                    return new AIResponse(hintText, false, null);
                })
                .onErrorResume(WebClientResponseException.class,
                        (WebClientResponseException ex) -> {
                            String details = ex.getResponseBodyAsString();
                            if (details == null || details.isBlank()) {
                                details = ex.getMessage();
                            }
                            return Mono.just(new AIResponse(
                                    "Erro da API Gemini (" + ex.getStatusCode().value() + "): " + details,
                                    false,
                                    null
                            ));
                        })
                .onErrorResume(Throwable.class, (Throwable ex) ->
                        Mono.just(new AIResponse("Erro inesperado: " + ex.getMessage(), false, null))
                );
    }

    // ===== EXTRACTORS =====

    @SuppressWarnings("unchecked")
    private Mono<AIResponse> extractAIResponseReactive(Map<String, Object> response) {
        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) response.getOrDefault("candidates", List.of());

        if (candidates.isEmpty()) {
            return Mono.just(new AIResponse("Resposta vazia da IA.", false, null));
        }

        Map<String, Object> firstCandidate = candidates.getFirst();
        Map<String, Object> content =
                (Map<String, Object>) firstCandidate.getOrDefault("content", Map.of());

        List<Map<String, Object>> parts =
                (List<Map<String, Object>>) content.getOrDefault("parts", List.of());

        String text = parts.isEmpty()
                ? "Resposta inválida da IA."
                : String.valueOf(parts.getFirst().getOrDefault("text", "")).trim();

        conversationHistory += "\nIA: " + text;

        boolean won = text.startsWith("Sim! O personagem é");
        if (!won) {
            return Mono.just(new AIResponse(text, false, null));
        }

        String name = extract(text, "Sim! O personagem é", ".");
        String work = extract(text, "Obra:", "\n");

        String nameOk = name == null ? "" : name;
        String workOk = work == null ? "" : work;

        String query = (nameOk + " " + workOk + " character official portrait").trim();

        return imageSearchService.searchImage(query)
                .defaultIfEmpty("")
                .map(imageUrl -> {
                    CharacterData data = new CharacterData(nameOk, workOk, imageUrl);
                    String answerText = "Sim! O personagem é " + nameOk + ".\nObra: " + workOk;
                    return new AIResponse(answerText, true, data);
                });
    }

    // ✅ Novo: extrai texto simples (pra hints)
    @SuppressWarnings("unchecked")
    private Mono<String> extractTextOnlyReactive(Map<String, Object> response) {
        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) response.getOrDefault("candidates", List.of());

        if (candidates.isEmpty()) return Mono.just("Não consegui gerar uma dica agora.");

        Map<String, Object> firstCandidate = candidates.getFirst();
        Map<String, Object> content =
                (Map<String, Object>) firstCandidate.getOrDefault("content", Map.of());

        List<Map<String, Object>> parts =
                (List<Map<String, Object>>) content.getOrDefault("parts", List.of());

        String text = parts.isEmpty()
                ? ""
                : String.valueOf(parts.getFirst().getOrDefault("text", "")).trim();

        if (text.isBlank()) return Mono.just("Não consegui gerar uma dica agora.");

        // remove prefixos comuns
        text = text.replaceAll("^([Dd]ica\\s*:\\s*)", "").trim();

        return Mono.just(text);
    }

    private String extract(String text, String startToken, String endToken) {
        try {
            int i = text.indexOf(startToken);
            if (i < 0) return null;

            int start = i + startToken.length();
            int j = text.indexOf(endToken, start);

            if (j < 0) j = text.length();
            return text.substring(start, j).trim();
        } catch (Exception ignored) {
            return null;
        }
    }
}
