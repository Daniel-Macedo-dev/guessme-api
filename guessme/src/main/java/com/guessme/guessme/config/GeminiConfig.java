package com.guessme.guessme.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Loads the Gemini API key from (in order of precedence):
 *   1. Environment variable GEMINI_API_KEY
 *   2. gemini.properties on the classpath (local dev — not committed)
 *
 * If neither is set the key will be blank and GameService returns a
 * user-visible error message without crashing the application context.
 *
 * To set up locally: copy gemini.properties.example → gemini.properties
 * and replace the placeholder value with your real key.
 */
@Configuration
@PropertySource(value = "classpath:gemini.properties", ignoreResourceNotFound = true)
public class GeminiConfig {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Bean
    public WebClient geminiWebClient() {
        return WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("x-goog-api-key", geminiApiKey)
                .build();
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }
}
