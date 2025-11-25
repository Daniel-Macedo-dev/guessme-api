package com.guessme.guessme.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class ImageLookupService {

    private final WebClient webClient;

    @Value("${google.kg.api.key}")
    private String apiKey;

    public ImageLookupService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://kgsearch.googleapis.com")
                .build();
    }

    public Mono<String> findImageUrl(String query) {
        String q = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "/v1/entities:search?query=" + q + "&limit=1&key=" + apiKey + "&languages=pt";

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> {
                    List<Map<String, Object>> items = (List<Map<String, Object>>) resp.get("itemListElement");
                    if (items == null || items.isEmpty()) return null;
                    Map<String, Object> first = (Map<String, Object>) items.get(0).get("result");
                    if (first == null) return null;
                    Object imageObj = first.get("image");
                    if (imageObj instanceof Map<?, ?>) {
                        Object contentUrl = ((Map<?, ?>) imageObj).get("contentUrl");
                        if (contentUrl instanceof String) {
                            return (String) contentUrl;
                        }
                    }
                    return null;
                })
                .onErrorReturn(null);
    }
}
