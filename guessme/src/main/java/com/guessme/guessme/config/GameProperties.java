package com.guessme.guessme.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "game")
public class GameProperties {

    private int maxQuestionLength = 300;
    private int maxSessions = 200;
    private long sessionTtlMinutes = 60;
    private int maxQuestionsPerSession = 50;
    private int maxHintsPerSession = 10;
    private long requestCooldownMs = 3000;
}
