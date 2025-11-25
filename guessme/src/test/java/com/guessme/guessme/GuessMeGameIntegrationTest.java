package com.guessme.guessme;

import com.guessme.guessme.dto.AIResponse;
import com.guessme.guessme.dto.CharacterData;
import com.guessme.guessme.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class GuessMeGameIntegrationTest {

    @Autowired
    private GameService gameService;

    @Test
    void testMotokoVictory() {

        AIResponse startResponse = gameService.startGame().block();
        System.out.println("IA inicial: " + startResponse.answer());

        AIResponse aiResponse = gameService.askAI(
                "O personagem é Motoko Kusanagi, de Ghost in the Shell?"
        ).block();

        System.out.println("Resposta IA: " + aiResponse.answer());
        if (aiResponse.success()) {
            CharacterData c = aiResponse.character();
            System.out.println("=== RESPOSTA DETECTADA ===");
            System.out.println("Nome: " + c.name());
            System.out.println("Obra: " + c.work());
            System.out.println("Imagem: " + c.image());
        }
    }
}
