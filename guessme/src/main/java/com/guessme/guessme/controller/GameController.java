package com.guessme.guessme.controller;

import com.guessme.guessme.service.GameService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*") // libera requisições do React
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping(value = "/start", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> startGame() {
        String prompt = """
            Você é o sistema do jogo "Guess Me".
            Escolha um personagem famoso (real ou fictício), mas não diga quem é.
            Responda apenas com "Sim", "Não" ou "Talvez" até o jogador acertar.
            Quando ele acertar, confirme e encerre.
            Comece dizendo: "Ok! Já escolhi um personagem. Pode fazer sua primeira pergunta!"
            """;
        return gameService.askAI(prompt);
    }

    @PostMapping(value = "/ask", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> askAI(@RequestBody String question) {
        return gameService.askAI(question);
    }
}
