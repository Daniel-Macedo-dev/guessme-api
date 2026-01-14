package com.guessme.guessme.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@PropertySource("classpath:gemini.properties")
public class GeminiConfig {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Bean
    public WebClient geminiWebClient() {
        return WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1")
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("x-goog-api-key", geminiApiKey)
                .build();
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }
}
