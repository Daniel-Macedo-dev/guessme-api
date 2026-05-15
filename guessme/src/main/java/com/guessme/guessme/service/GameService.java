package com.guessme.guessme.service;

import com.guessme.guessme.config.GeminiConfig;
import com.guessme.guessme.dto.AIResponse;
import com.guessme.guessme.dto.CharacterData;
import com.guessme.guessme.model.GameSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class GameService {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    // Callers that omit sessionId share this fallback (backward compat for local testing).
    static final String DEFAULT_SESSION_ID = "default";
    private static final int MAX_SESSIONS = 200;
    private static final long SESSION_TTL_MINUTES = 60;

    private final GeminiConfig geminiConfig;
    private final WebClient geminiWebClient;
    private final ImageSearchService imageSearchService;

    @Value("${gemini.model:gemini-3.1-flash-lite}")
    private String geminiModel;

    private final ConcurrentHashMap<String, GameSession> sessions = new ConcurrentHashMap<>();

    public List<String> getCategories() {
        return List.of("Geral", "Anime", "Games", "Filmes", "Séries", "Quadrinhos");
    }

    // Backward-compatible no-arg variant kept for existing callers.
    public Mono<AIResponse> startGame() {
        return startGame(null);
    }

    public Mono<AIResponse> startGame(String category) {
        String sessionId = UUID.randomUUID().toString();
        GameSession session = new GameSession();
        session.setCurrentCategory(
                (category == null || category.isBlank()) ? "Geral" : category.trim()
        );

        String categoryName = session.getCurrentCategory();
        String text = "Ok! Já escolhi um personagem"
                + (categoryName.equalsIgnoreCase("Geral") ? "" : " da categoria: " + categoryName)
                + ". Pode fazer sua primeira pergunta!";

        session.appendHistory("\n[SISTEMA] Categoria: " + categoryName);
        session.appendHistory("\nIA: " + text);

        evictIfNeeded();
        sessions.put(sessionId, session);

        return Mono.just(new AIResponse(text, false, null, sessionId));
    }

    // Backward-compatible single-arg variant; routes to the default session.
    public Mono<AIResponse> askAI(String question) {
        return askAI(question, null);
    }

    public Mono<AIResponse> askAI(String question, String sessionId) {
        if (question == null || question.isBlank()) {
            return Mono.just(new AIResponse("Pergunta inválida ou vazia.", false, null, sessionId));
        }

        GameSession session = resolveSession(sessionId);
        if (session == null) {
            return Mono.just(new AIResponse(
                    "Sessão não encontrada. Inicie um novo jogo com POST /api/game/start.",
                    false, null, sessionId
            ));
        }

        String key = geminiConfig.getGeminiApiKey();
        if (key == null || key.isBlank()) {
            return Mono.just(new AIResponse(
                    "Config inválida: gemini.api.key não foi carregada (gemini.properties).",
                    false, null, sessionId
            ));
        }

        session.appendHistory("\nUsuário: " + question);

        String categoryRule = """
                Regras de universo/categoria:
                - Categoria atual: %s
                - Você deve manter UM personagem secreto dentro dessa categoria (ou equivalente).
                - Não revele nome/obra diretamente, a não ser que o jogador acerte.
                """.formatted(session.getCurrentCategory());

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
                        + session.getConversationHistory()
                        + "\nAgora responda seguindo as regras.";

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", finalPrompt)
                        ))
                )
        );

        final String resolvedId = resolveId(sessionId);
        final GameSession finalSession = session;

        return geminiWebClient.post()
                .uri("/models/" + geminiModel + ":generateContent")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(MAP_TYPE)
                .flatMap(responseMap -> extractAIResponseReactive(responseMap, finalSession, resolvedId))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    String details = ex.getResponseBodyAsString();
                    if (details == null || details.isBlank()) details = ex.getMessage();
                    return Mono.just(new AIResponse(
                            "Erro da API Gemini (" + ex.getStatusCode().value() + "): " + details,
                            false, null, resolvedId
                    ));
                })
                .onErrorResume(Throwable.class, ex ->
                        Mono.just(new AIResponse("Erro inesperado: " + ex.getMessage(), false, null, resolvedId))
                );
    }

    // Backward-compatible no-arg variant; routes to the default session.
    public Mono<AIResponse> hint() {
        return hint(null);
    }

    public Mono<AIResponse> hint(String sessionId) {
        GameSession session = resolveSession(sessionId);
        if (session == null) {
            return Mono.just(new AIResponse(
                    "Sessão não encontrada. Inicie um novo jogo com POST /api/game/start.",
                    false, null, sessionId
            ));
        }

        String key = geminiConfig.getGeminiApiKey();
        if (key == null || key.isBlank()) {
            return Mono.just(new AIResponse(
                    "Config inválida: gemini.api.key não foi carregada (gemini.properties).",
                    false, null, sessionId
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
                """.formatted(session.getCurrentCategory())
                        + session.getConversationHistory()
                        + "\nResponda apenas com a dica (sem prefixos).";

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        final String resolvedId = resolveId(sessionId);
        final GameSession finalSession = session;

        return geminiWebClient.post()
                .uri("/models/" + geminiModel + ":generateContent")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(MAP_TYPE)
                .flatMap(this::extractTextOnlyReactive)
                .map(hintText -> {
                    finalSession.appendHistory("\nIA (DICA): " + hintText);
                    return new AIResponse(hintText, false, null, resolvedId);
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    String details = ex.getResponseBodyAsString();
                    if (details == null || details.isBlank()) details = ex.getMessage();
                    return Mono.just(new AIResponse(
                            "Erro da API Gemini (" + ex.getStatusCode().value() + "): " + details,
                            false, null, resolvedId
                    ));
                })
                .onErrorResume(Throwable.class, ex ->
                        Mono.just(new AIResponse("Erro inesperado: " + ex.getMessage(), false, null, resolvedId))
                );
    }

    // ===== HELPERS =====

    /**
     * Returns the session for the given ID, or the shared default session when
     * sessionId is absent (backward-compat). Returns null only for an explicit
     * but unrecognized session ID, so the caller can return a "session not found" error.
     */
    GameSession resolveSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return sessions.computeIfAbsent(DEFAULT_SESSION_ID, k -> new GameSession());
        }
        GameSession s = sessions.get(sessionId);
        if (s != null) s.touch();
        return s;
    }

    private String resolveId(String sessionId) {
        return (sessionId == null || sessionId.isBlank()) ? DEFAULT_SESSION_ID : sessionId;
    }

    private void evictIfNeeded() {
        if (sessions.size() < MAX_SESSIONS) return;
        Instant cutoff = Instant.now().minus(SESSION_TTL_MINUTES, ChronoUnit.MINUTES);
        sessions.entrySet().removeIf(e ->
                !e.getKey().equals(DEFAULT_SESSION_ID)
                        && e.getValue().getLastAccess().isBefore(cutoff)
        );
    }

    // ===== EXTRACTORS =====

    @SuppressWarnings("unchecked")
    private Mono<AIResponse> extractAIResponseReactive(
            Map<String, Object> response, GameSession session, String sessionId) {

        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) response.getOrDefault("candidates", List.of());

        if (candidates.isEmpty()) {
            return Mono.just(new AIResponse("Resposta vazia da IA.", false, null, sessionId));
        }

        Map<String, Object> firstCandidate = candidates.getFirst();
        Map<String, Object> content =
                (Map<String, Object>) firstCandidate.getOrDefault("content", Map.of());

        List<Map<String, Object>> parts =
                (List<Map<String, Object>>) content.getOrDefault("parts", List.of());

        String text = parts.isEmpty()
                ? "Resposta inválida da IA."
                : String.valueOf(parts.getFirst().getOrDefault("text", "")).trim();

        session.appendHistory("\nIA: " + text);

        boolean won = text.startsWith("Sim! O personagem é");
        if (!won) {
            return Mono.just(new AIResponse(text, false, null, sessionId));
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
                    return new AIResponse(answerText, true, data, sessionId);
                });
    }

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
