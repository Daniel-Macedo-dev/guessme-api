package com.guessme.guessme.config;

import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "game")
public class GameProperties implements InitializingBean {

    private int maxQuestionLength = 300;
    private int maxSessions = 200;
    private long sessionTtlMinutes = 60;
    private int maxQuestionsPerSession = 50;
    private int maxHintsPerSession = 10;
    private long requestCooldownMs = 3000;

    @Override
    public void afterPropertiesSet() {
        requirePositive(maxQuestionLength, "game.max-question-length");
        requirePositive(maxSessions, "game.max-sessions");
        requirePositive(sessionTtlMinutes, "game.session-ttl-minutes");
        requirePositive(maxQuestionsPerSession, "game.max-questions-per-session");
        requirePositive(maxHintsPerSession, "game.max-hints-per-session");
        if (requestCooldownMs < 0) {
            throw new IllegalStateException("game.request-cooldown-ms must be zero or greater");
        }
    }

    private static void requirePositive(long value, String property) {
        if (value <= 0) throw new IllegalStateException(property + " must be greater than zero");
    }
}
