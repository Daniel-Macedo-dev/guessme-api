package com.guessme.guessme.config;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAIConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient();
    }

    @Bean
    public String openAiKey() {
        String key = System.getenv("OPENAI_API_KEY");
        if (key == null || key.isEmpty()) {
            throw new IllegalStateException("A variável de ambiente OPENAI_API_KEY não está definida!");
        }
        return key;
    }
}
