package com.guessme.guessme;

import com.guessme.guessme.dto.AIResponse;
import com.guessme.guessme.dto.CharacterData;
import com.guessme.guessme.service.GameService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live integration test — requires real API keys in gemini.properties and google.properties.
 * Excluded from normal 'mvn test' by the @Tag("live") Surefire filter.
 * Run manually: mvn test -Dgroups=live -Dsurefire.excludedGroups=""
 * No assertions: output is printed for manual review.
 */
@Tag("live")
@SpringBootTest
public class GuessMeGameIntegrationTest {

    @Autowired
    private GameService gameService;

    @Test
    void testMotokoVictory() {
        AIResponse startResponse = gameService.startGame().block();
        assertNotNull(startResponse, "start response must not be null");
        assertNotNull(startResponse.sessionId(), "sessionId must be present");
        assertFalse(startResponse.sessionId().isBlank(), "sessionId must not be blank");
        assertFalse(startResponse.answer().isBlank(), "start answer must not be blank");
        System.out.println("IA inicial: " + startResponse.answer());

        String sessionId = startResponse.sessionId();

        AIResponse aiResponse = gameService.askAI(
                "O personagem é Motoko Kusanagi, de Ghost in the Shell?",
                sessionId
        ).block();

        assertNotNull(aiResponse, "ask response must not be null");
        assertFalse(aiResponse.answer().isBlank(), "ask answer must not be blank");
        System.out.println("Resposta IA: " + aiResponse.answer());
        if (aiResponse.success()) {
            CharacterData c = aiResponse.character();
            assertNotNull(c, "character data must be present on success");
            System.out.println("=== RESPOSTA DETECTADA ===");
            System.out.println("Nome: " + c.name());
            System.out.println("Obra: " + c.work());
            System.out.println("Imagem: " + c.image());
        }
    }
}
