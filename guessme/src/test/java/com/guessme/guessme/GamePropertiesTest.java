package com.guessme.guessme;

import com.guessme.guessme.config.GameProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GamePropertiesTest {

    @Test
    void defaultsAreValid() {
        assertDoesNotThrow(() -> new GameProperties().afterPropertiesSet());
    }

    @Test
    void rejectsNonPositiveSessionCapacity() {
        GameProperties properties = new GameProperties();
        properties.setMaxSessions(0);

        assertThrows(IllegalStateException.class, properties::afterPropertiesSet);
    }

    @Test
    void allowsDisabledCooldown() {
        GameProperties properties = new GameProperties();
        properties.setRequestCooldownMs(0);

        assertDoesNotThrow(properties::afterPropertiesSet);
    }

    @Test
    void rejectsNegativeCooldown() {
        GameProperties properties = new GameProperties();
        properties.setRequestCooldownMs(-1);

        assertThrows(IllegalStateException.class, properties::afterPropertiesSet);
    }
}
