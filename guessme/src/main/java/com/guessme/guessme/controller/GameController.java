package com.guessme.guessme.controller;

import com.guessme.guessme.dto.AIResponse;
import com.guessme.guessme.dto.HintDTO;
import com.guessme.guessme.dto.QuestionDTO;
import com.guessme.guessme.dto.StartDTO;
import com.guessme.guessme.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * API contract for frontend:
 *
 *  POST /api/game/start      body: {"category":"Anime"}  (optional)
 *    → response includes "sessionId" — store it and send with every subsequent request.
 *
 *  POST /api/game/ask        body: {"question":"É humano?","sessionId":"<id>"}
 *  POST /api/game/hint       body: {"sessionId":"<id>"}
 *
 * sessionId is optional for backward-compat local testing; omitting it routes
 * to a shared default session (not safe for concurrent multi-user scenarios).
 */
@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @GetMapping("/categories")
    public List<String> categories() {
        return gameService.getCategories();
    }

    @GetMapping("/start")
    public Mono<AIResponse> startGame(@RequestParam(required = false) String category) {
        return gameService.startGame(category);
    }

    @PostMapping("/start")
    public Mono<AIResponse> startGamePost(@RequestBody(required = false) StartDTO dto) {
        String category = (dto == null) ? null : dto.category();
        return gameService.startGame(category);
    }

    @PostMapping("/ask")
    public Mono<AIResponse> askAI(@RequestBody(required = false) QuestionDTO dto) {
        if (dto == null || dto.question() == null || dto.question().isBlank()) {
            return Mono.just(new AIResponse("Pergunta inválida ou vazia.", false, null, null));
        }
        return gameService.askAI(dto.question(), dto.sessionId());
    }

    @PostMapping("/hint")
    public Mono<AIResponse> hint(@RequestBody(required = false) HintDTO dto) {
        String sessionId = (dto == null) ? null : dto.sessionId();
        return gameService.hint(sessionId);
    }
}
