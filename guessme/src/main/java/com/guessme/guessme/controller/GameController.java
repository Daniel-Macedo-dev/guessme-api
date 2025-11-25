package com.guessme.guessme.controller;

import com.guessme.guessme.dto.AIResponse;
import com.guessme.guessme.dto.QuestionDTO;
import com.guessme.guessme.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @GetMapping("/start")
    public Mono<AIResponse> startGame() {
        return gameService.startGame();
    }

    @PostMapping("/ask")
    public Mono<AIResponse> askAI(@RequestBody QuestionDTO dto) {
        return gameService.askAI(dto.question());
    }
}
