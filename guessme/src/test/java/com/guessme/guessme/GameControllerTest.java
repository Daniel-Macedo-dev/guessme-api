package com.guessme.guessme;

import com.guessme.guessme.dto.AIResponse;
import com.guessme.guessme.dto.CharacterData;
import com.guessme.guessme.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.Mockito.when;

/**
 * Controller-level tests. GameService is mocked so no Gemini or Google API calls are made.
 * Covers the HTTP contract: status codes, response shape, and input validation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
class GameControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private GameService gameService;

    // ── GET /api/game/categories ────────────────────────────────────────────

    @Test
    void categories_returns200WithAllCategories() {
        when(gameService.getCategories()).thenReturn(
                List.of("Geral", "Anime", "Games", "Filmes", "Séries", "Quadrinhos")
        );

        webTestClient.get().uri("/api/game/categories")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0]").isEqualTo("Geral")
                .jsonPath("$[1]").isEqualTo("Anime")
                .jsonPath("$[5]").isEqualTo("Quadrinhos");
    }

    // ── GET /api/game/start ─────────────────────────────────────────────────

    @Test
    void getStart_noCategory_returnsAIResponseWithSessionId() {
        AIResponse stub = new AIResponse("Pronto! Pode perguntar.", false, null, "sess-abc");
        when(gameService.startGame(null)).thenReturn(Mono.just(stub));

        webTestClient.get().uri("/api/game/start")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.answer").isEqualTo("Pronto! Pode perguntar.")
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.character").doesNotExist()
                .jsonPath("$.sessionId").isEqualTo("sess-abc");
    }

    @Test
    void getStart_withCategoryParam_forwardsToService() {
        AIResponse stub = new AIResponse("Anime escolhido.", false, null, "sess-anime");
        when(gameService.startGame("Anime")).thenReturn(Mono.just(stub));

        webTestClient.get().uri("/api/game/start?category=Anime")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.sessionId").isEqualTo("sess-anime");
    }

    // ── POST /api/game/start ────────────────────────────────────────────────

    @Test
    void postStart_withCategoryBody_returnsSessionId() {
        AIResponse stub = new AIResponse("Games escolhido.", false, null, "sess-games");
        when(gameService.startGame("Games")).thenReturn(Mono.just(stub));

        webTestClient.post().uri("/api/game/start")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"category\":\"Games\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.sessionId").isEqualTo("sess-games")
                .jsonPath("$.success").isEqualTo(false);
    }

    @Test
    void postStart_noBody_usesNullCategory() {
        AIResponse stub = new AIResponse("Geral.", false, null, "sess-geral");
        when(gameService.startGame(null)).thenReturn(Mono.just(stub));

        webTestClient.post().uri("/api/game/start")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.sessionId").isEqualTo("sess-geral");
    }

    // ── POST /api/game/ask ──────────────────────────────────────────────────

    @Test
    void ask_missingQuestion_returnsValidationError() {
        // Body {} → QuestionDTO(question=null, sessionId=null).
        // Controller guard returns error without calling service.
        webTestClient.post().uri("/api/game/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.answer").isNotEmpty();
    }

    @Test
    void ask_blankQuestion_returnsValidationError() {
        webTestClient.post().uri("/api/game/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"question\":\"   \",\"sessionId\":\"sess-abc\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.answer").isNotEmpty();
    }

    @Test
    void ask_validQuestion_delegatesToService() {
        AIResponse stub = new AIResponse("Não", false, null, "sess-abc");
        when(gameService.askAI("É humano?", "sess-abc")).thenReturn(Mono.just(stub));

        webTestClient.post().uri("/api/game/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"question\":\"É humano?\",\"sessionId\":\"sess-abc\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.answer").isEqualTo("Não")
                .jsonPath("$.sessionId").isEqualTo("sess-abc");
    }

    @Test
    void ask_winResponse_returnsCharacterData() {
        CharacterData character = new CharacterData(
                "Naruto Uzumaki", "Naruto", "https://img.example.com/naruto.jpg");
        AIResponse stub = new AIResponse(
                "Sim! O personagem é Naruto Uzumaki.\nObra: Naruto", true, character, "sess-abc");
        when(gameService.askAI("É o Naruto?", "sess-abc")).thenReturn(Mono.just(stub));

        webTestClient.post().uri("/api/game/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"question\":\"É o Naruto?\",\"sessionId\":\"sess-abc\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.character.name").isEqualTo("Naruto Uzumaki")
                .jsonPath("$.character.work").isEqualTo("Naruto")
                .jsonPath("$.character.image").isNotEmpty();
    }

    // ── POST /api/game/hint ─────────────────────────────────────────────────

    @Test
    void hint_withSessionId_delegatesToService() {
        AIResponse stub = new AIResponse("Este personagem usa uma armadura.", false, null, "sess-abc");
        when(gameService.hint("sess-abc")).thenReturn(Mono.just(stub));

        webTestClient.post().uri("/api/game/hint")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"sessionId\":\"sess-abc\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.answer").isEqualTo("Este personagem usa uma armadura.")
                .jsonPath("$.sessionId").isEqualTo("sess-abc");
    }

    @Test
    void hint_noSessionId_delegatesToServiceWithNull() {
        // Body {} → HintDTO(sessionId=null) → service.hint(null) for backward-compat path
        AIResponse stub = new AIResponse("Dica geral.", false, null, "default");
        when(gameService.hint(null)).thenReturn(Mono.just(stub));

        webTestClient.post().uri("/api/game/hint")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.answer").isEqualTo("Dica geral.");
    }
}
