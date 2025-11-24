package com.guessme.guessme;

import com.guessme.guessme.dto.AIResponse;
import com.guessme.guessme.dto.CharacterData;
import com.guessme.guessme.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;

@SpringBootTest
public class GuessMeGameIntegrationTest {

    @Autowired
    private GameService gameService;

    @Test
    void testMotokoVictory() {
        Mono<AIResponse> startMono = gameService.startGame();

        startMono.subscribe(startResponse -> {
            System.out.println("IA inicial: " + startResponse.text());

            Mono<AIResponse> askMono = gameService.askAI("O personagem é Motoko Kusanagi?");
            askMono.subscribe(aiResponse -> {
                System.out.println("Resposta IA: " + aiResponse.text());
                if (aiResponse.vitoria()) {
                    CharacterData winner = aiResponse.dados();
                    System.out.println("Nome: " + winner.nome());
                    System.out.println("Obra: " + winner.obra());
                    System.out.println("Imagem: " + winner.imagem());
                }
            });
        });

        try { Thread.sleep(10000); } catch (InterruptedException e) { e.printStackTrace(); }
    }
}
