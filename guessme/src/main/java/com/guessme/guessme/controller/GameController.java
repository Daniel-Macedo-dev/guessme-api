package com.guessme.guessme.controller;

import com.guessme.guessme.dto.AIResponse;
import com.guessme.guessme.dto.QuestionDTO;
import com.guessme.guessme.dto.StartDTO;
import com.guessme.guessme.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

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
    public Mono<AIResponse> askAI(@RequestBody QuestionDTO dto) {
        return gameService.askAI(dto.question());
    }

    @PostMapping("/hint")
    public Mono<AIResponse> hint() {
        return gameService.hint();
    }
}
