package com.guessme.guessme;

import com.guessme.guessme.config.GeminiConfig;
import com.guessme.guessme.dto.AIResponse;
import com.guessme.guessme.model.GameSession;
import com.guessme.guessme.service.GameService;
import com.guessme.guessme.service.ImageSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock private GeminiConfig geminiConfig;
    @Mock private WebClient geminiWebClient;
    @Mock private ImageSearchService imageSearchService;

    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameService(geminiConfig, geminiWebClient, imageSearchService);
        ReflectionTestUtils.setField(gameService, "geminiModel", "gemini-2.0-flash-lite");
    }

    // --- blank / null question validation ---

    @Test
    void askAI_blankQuestion_returnsErrorWithoutCallingApi() {
        AIResponse start = gameService.startGame(null).block();
        assertNotNull(start);
        String sessionId = start.sessionId();

        AIResponse result = gameService.askAI("   ", sessionId).block();

        assertNotNull(result);
        assertFalse(result.success());
        assertFalse(result.answer().isBlank());
        // WebClient must never have been touched — Mockito would fail if it were called
        // without stubbing (strict stubs mode is active via MockitoExtension).
    }

    @Test
    void askAI_nullQuestion_returnsError() {
        AIResponse start = gameService.startGame(null).block();
        assertNotNull(start);

        AIResponse result = gameService.askAI(null, start.sessionId()).block();

        assertNotNull(result);
        assertFalse(result.success());
        assertFalse(result.answer().isBlank());
    }

    // --- start game ---

    @Test
    void startGame_noCategory_defaultsToGeral() {
        AIResponse result = gameService.startGame(null).block();

        assertNotNull(result);
        assertFalse(result.success());
        assertNotNull(result.sessionId());
        assertFalse(result.answer().isBlank());
        // message should not mention a specific category
        assertFalse(result.answer().contains("da categoria:"));
    }

    @Test
    void startGame_withCategory_categoryAppearsInMessage() {
        AIResponse result = gameService.startGame("Anime").block();

        assertNotNull(result);
        assertNotNull(result.sessionId());
        assertTrue(result.answer().contains("Anime"));
    }

    @Test
    void startGame_blankCategory_defaultsToGeral() {
        AIResponse result = gameService.startGame("   ").block();

        assertNotNull(result);
        assertFalse(result.answer().contains("da categoria:"));
    }

    // --- session isolation ---

    @Test
    void twoStartCalls_returnDistinctSessionIds() {
        AIResponse a = gameService.startGame("Anime").block();
        AIResponse b = gameService.startGame("Games").block();

        assertNotNull(a);
        assertNotNull(b);
        assertNotNull(a.sessionId());
        assertNotNull(b.sessionId());
        assertNotEquals(a.sessionId(), b.sessionId());
    }

    @Test
    void askAI_unknownSessionId_returnsSessionNotFoundError() {
        AIResponse result = gameService.askAI("É humano?", "non-existent-uuid").block();

        assertNotNull(result);
        assertFalse(result.success());
        assertTrue(result.answer().toLowerCase().contains("sess"));
    }

    @Test
    void geminiModel_isInjected() {
        String model = (String) ReflectionTestUtils.getField(gameService, "geminiModel");
        assertNotNull(model, "geminiModel must not be null after setUp");
        assertFalse(model.isBlank(), "geminiModel must not be blank");
    }

    // --- missing Gemini API key ---

    @Test
    void askAI_missingGeminiKey_returnsConfigError() {
        AIResponse start = gameService.startGame(null).block();
        assertNotNull(start);

        when(geminiConfig.getGeminiApiKey()).thenReturn("");

        AIResponse result = gameService.askAI("É humano?", start.sessionId()).block();

        assertNotNull(result);
        assertFalse(result.success());
        assertTrue(result.answer().contains("gemini.api.key"));
        // geminiWebClient is never touched — strict stubs would fail if it were called
    }

    @Test
    void hint_missingGeminiKey_returnsConfigError() {
        AIResponse start = gameService.startGame(null).block();
        assertNotNull(start);

        when(geminiConfig.getGeminiApiKey()).thenReturn("");

        AIResponse result = gameService.hint(start.sessionId()).block();

        assertNotNull(result);
        assertFalse(result.success());
        assertTrue(result.answer().contains("gemini.api.key"));
    }

    // --- hint session handling ---

    @Test
    void hint_unknownSessionId_returnsSessionNotFoundError() {
        AIResponse result = gameService.hint("non-existent-uuid").block();

        assertNotNull(result);
        assertFalse(result.success());
        assertTrue(result.answer().toLowerCase().contains("sess"));
    }

    // --- AI win-response parsing (canned Gemini responses, no real API call) ---

    @Test
    void askAI_standardWinResponse_returnsSuccessWithCharacterData() {
        AIResponse start = gameService.startGame(null).block();
        assertNotNull(start);
        when(geminiConfig.getGeminiApiKey()).thenReturn("fake-key");
        when(imageSearchService.searchImage(anyString())).thenReturn(Mono.just(""));
        stubGemini(Mono.just(fakeGeminiResponse("Sim! O personagem é Naruto. Obra: Naruto")));

        AIResponse result = gameService.askAI("É o Naruto?", start.sessionId()).block();

        assertNotNull(result);
        assertTrue(result.success());
        assertNotNull(result.character());
        assertEquals("Naruto", result.character().name());
        assertEquals("Naruto", result.character().work());
    }

    @Test
    void askAI_winResponseWithLeadingTrailingSpaces_returnsSuccess() {
        AIResponse start = gameService.startGame(null).block();
        assertNotNull(start);
        when(geminiConfig.getGeminiApiKey()).thenReturn("fake-key");
        when(imageSearchService.searchImage(anyString())).thenReturn(Mono.just(""));
        stubGemini(Mono.just(fakeGeminiResponse("  Sim! O personagem é Naruto. Obra: Naruto  ")));

        AIResponse result = gameService.askAI("É o Naruto?", start.sessionId()).block();

        assertNotNull(result);
        assertTrue(result.success());
        assertEquals("Naruto", result.character().name());
        assertEquals("Naruto", result.character().work());
    }

    @Test
    void askAI_winResponseWithoutPeriodAfterName_returnsSuccess() {
        AIResponse start = gameService.startGame(null).block();
        assertNotNull(start);
        when(geminiConfig.getGeminiApiKey()).thenReturn("fake-key");
        when(imageSearchService.searchImage(anyString())).thenReturn(Mono.just(""));
        stubGemini(Mono.just(fakeGeminiResponse("Sim! O personagem é Naruto\nObra: Naruto")));

        AIResponse result = gameService.askAI("É o Naruto?", start.sessionId()).block();

        assertNotNull(result);
        assertTrue(result.success());
        assertEquals("Naruto", result.character().name());
        assertEquals("Naruto", result.character().work());
    }

    @Test
    void askAI_winResponseWithLineBreakBeforeObra_returnsSuccess() {
        AIResponse start = gameService.startGame(null).block();
        assertNotNull(start);
        when(geminiConfig.getGeminiApiKey()).thenReturn("fake-key");
        when(imageSearchService.searchImage(anyString())).thenReturn(Mono.just(""));
        stubGemini(Mono.just(fakeGeminiResponse("Sim! O personagem é Naruto.\nObra: Naruto")));

        AIResponse result = gameService.askAI("É o Naruto?", start.sessionId()).block();

        assertNotNull(result);
        assertTrue(result.success());
        assertEquals("Naruto", result.character().name());
        assertEquals("Naruto", result.character().work());
    }

    @Test
    void askAI_nonWinResponseNao_returnsSuccessFalse() {
        AIResponse start = gameService.startGame(null).block();
        assertNotNull(start);
        when(geminiConfig.getGeminiApiKey()).thenReturn("fake-key");
        stubGemini(Mono.just(fakeGeminiResponse("Não")));

        AIResponse result = gameService.askAI("É humano?", start.sessionId()).block();

        assertNotNull(result);
        assertFalse(result.success());
        assertNull(result.character());
        assertEquals("Não", result.answer());
    }

    @Test
    void askAI_emptyCandidates_returnsEmptyResponseBehavior() {
        AIResponse start = gameService.startGame(null).block();
        assertNotNull(start);
        when(geminiConfig.getGeminiApiKey()).thenReturn("fake-key");
        stubGemini(Mono.just(Map.of("candidates", List.of())));

        AIResponse result = gameService.askAI("É humano?", start.sessionId()).block();

        assertNotNull(result);
        assertFalse(result.success());
        assertNull(result.character());
        assertFalse(result.answer().isBlank());
    }

    @Test
    void askAI_malformedCandidateMissingContent_returnsInvalidResponseBehavior() {
        AIResponse start = gameService.startGame(null).block();
        assertNotNull(start);
        when(geminiConfig.getGeminiApiKey()).thenReturn("fake-key");
        stubGemini(Mono.just(Map.of("candidates", List.of(Map.of()))));

        AIResponse result = gameService.askAI("É humano?", start.sessionId()).block();

        assertNotNull(result);
        assertFalse(result.success());
        assertNull(result.character());
    }

    // --- test helpers ---

    private static Map<String, Object> fakeGeminiResponse(String text) {
        return Map.of("candidates", List.of(
                Map.of("content", Map.of("parts", List.of(
                        Map.of("text", text)
                )))
        ));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubGemini(Mono<Map<String, Object>> responseBody) {
        var uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        var bodySpec = mock(WebClient.RequestBodySpec.class);
        var headersSpec = mock(WebClient.RequestHeadersSpec.class);
        var responseSpec = mock(WebClient.ResponseSpec.class);

        when(geminiWebClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(responseBody);
    }

    // --- default-session fallback (null / blank sessionId) ---

    @Test
    void askAI_nullSessionId_usesDefaultSessionFallback() {
        // null sessionId must route to the shared default session, not return "session not found".
        // The config-error guard is used here to reach the key check without calling Gemini.
        when(geminiConfig.getGeminiApiKey()).thenReturn("");

        AIResponse result = gameService.askAI("É humano?", null).block();

        assertNotNull(result);
        assertFalse(result.success());
        assertTrue(result.answer().contains("gemini.api.key"),
                "null sessionId must resolve to the default session, not session-not-found");
    }

    @Test
    void askAI_blankSessionId_usesDefaultSessionFallback() {
        when(geminiConfig.getGeminiApiKey()).thenReturn("");

        AIResponse result = gameService.askAI("É humano?", "   ").block();

        assertNotNull(result);
        assertFalse(result.success());
        assertTrue(result.answer().contains("gemini.api.key"),
                "blank sessionId must resolve to the default session, not session-not-found");
    }

    @Test
    void hint_nullSessionId_usesDefaultSessionFallback() {
        when(geminiConfig.getGeminiApiKey()).thenReturn("");

        AIResponse result = gameService.hint(null).block();

        assertNotNull(result);
        assertFalse(result.success());
        assertTrue(result.answer().contains("gemini.api.key"),
                "hint with null sessionId must resolve to the default session, not session-not-found");
    }

    @Test
    void hint_blankSessionId_usesDefaultSessionFallback() {
        when(geminiConfig.getGeminiApiKey()).thenReturn("");

        AIResponse result = gameService.hint("  ").block();

        assertNotNull(result);
        assertFalse(result.success());
        assertTrue(result.answer().contains("gemini.api.key"),
                "hint with blank sessionId must resolve to the default session, not session-not-found");
    }

    // --- session eviction ---

    @Test
    @SuppressWarnings("unchecked")
    void eviction_staleSessionsRemovedWhenCapacityReached() {
        ConcurrentHashMap<String, GameSession> sessions =
                (ConcurrentHashMap<String, GameSession>) ReflectionTestUtils.getField(gameService, "sessions");

        // Fill exactly MAX_SESSIONS (200) stale entries so evictIfNeeded() fires on the next startGame.
        for (int i = 0; i < 200; i++) {
            GameSession stale = new GameSession();
            ReflectionTestUtils.setField(stale, "lastAccess", Instant.now().minus(2, ChronoUnit.HOURS));
            sessions.put("stale-" + i, stale);
        }

        gameService.startGame(null).block(); // triggers evictIfNeeded()

        assertFalse(sessions.containsKey("stale-0"), "Stale session 0 must be evicted");
        assertFalse(sessions.containsKey("stale-199"), "Stale session 199 must be evicted");
    }

    @Test
    @SuppressWarnings("unchecked")
    void eviction_freshSessionsSurviveEviction() {
        ConcurrentHashMap<String, GameSession> sessions =
                (ConcurrentHashMap<String, GameSession>) ReflectionTestUtils.getField(gameService, "sessions");

        // 199 stale + 1 fresh = 200 total, enough to trigger eviction.
        for (int i = 0; i < 199; i++) {
            GameSession stale = new GameSession();
            ReflectionTestUtils.setField(stale, "lastAccess", Instant.now().minus(2, ChronoUnit.HOURS));
            sessions.put("stale-" + i, stale);
        }
        sessions.put("fresh-session", new GameSession()); // lastAccess = now

        gameService.startGame(null).block();

        assertTrue(sessions.containsKey("fresh-session"), "Fresh session must survive eviction");
        assertFalse(sessions.containsKey("stale-0"), "Stale sessions must be removed");
    }

    @Test
    @SuppressWarnings("unchecked")
    void eviction_defaultSessionNeverEvictedEvenWhenStale() {
        ConcurrentHashMap<String, GameSession> sessions =
                (ConcurrentHashMap<String, GameSession>) ReflectionTestUtils.getField(gameService, "sessions");

        // Place a stale default session in the map.
        // "default" mirrors GameService.DEFAULT_SESSION_ID (package-private constant).
        GameSession staleDefault = new GameSession();
        ReflectionTestUtils.setField(staleDefault, "lastAccess", Instant.now().minus(2, ChronoUnit.HOURS));
        sessions.put("default", staleDefault);

        // Fill the rest to reach capacity and trigger eviction.
        for (int i = 0; i < 199; i++) {
            GameSession stale = new GameSession();
            ReflectionTestUtils.setField(stale, "lastAccess", Instant.now().minus(2, ChronoUnit.HOURS));
            sessions.put("stale-" + i, stale);
        }

        gameService.startGame(null).block();

        assertTrue(sessions.containsKey("default"),
                "DEFAULT_SESSION_ID must never be removed by eviction, even when stale");
        assertFalse(sessions.containsKey("stale-0"), "Other stale sessions must be evicted");
    }

    @Test
    @SuppressWarnings("unchecked")
    void eviction_doesNotRunBelowCapacity() {
        ConcurrentHashMap<String, GameSession> sessions =
                (ConcurrentHashMap<String, GameSession>) ReflectionTestUtils.getField(gameService, "sessions");

        // Add a small number of stale sessions — well below MAX_SESSIONS (200).
        for (int i = 0; i < 5; i++) {
            GameSession stale = new GameSession();
            ReflectionTestUtils.setField(stale, "lastAccess", Instant.now().minus(2, ChronoUnit.HOURS));
            sessions.put("stale-" + i, stale);
        }

        gameService.startGame(null).block();

        // Eviction guard (size < MAX_SESSIONS) prevents removal.
        assertTrue(sessions.containsKey("stale-0"),
                "Stale sessions must not be removed when total count is below MAX_SESSIONS");
    }

    @Test
    void sessionsAreIsolated_startingBDoesNotRemoveA() {
        AIResponse a = gameService.startGame("Anime").block();
        assertNotNull(a);

        // Start a second game — should not touch session A
        AIResponse b = gameService.startGame("Games").block();
        assertNotNull(b);

        // Session A must still exist: a blank question returns "invalid question",
        // NOT "session not found". If A were gone we'd get the not-found message.
        AIResponse checkA = gameService.askAI("", a.sessionId()).block();
        assertNotNull(checkA);
        assertFalse(checkA.answer().toLowerCase().contains("não encontrada"),
                "Session A should still exist after session B was started");

        // Session B must also be intact
        AIResponse checkB = gameService.askAI("", b.sessionId()).block();
        assertNotNull(checkB);
        assertFalse(checkB.answer().toLowerCase().contains("não encontrada"),
                "Session B should be accessible independently");
    }
}
