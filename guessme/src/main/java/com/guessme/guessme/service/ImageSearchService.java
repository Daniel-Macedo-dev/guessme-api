package com.guessme.guessme.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImageSearchService {

    @Value("${google.api.key}")
    private String apiKey;

    @Value("${google.search.cx}")

    private String cx;

    private final WebClient webClient = WebClient.create("https://www.googleapis.com");

    public String searchImage(String query) {
        try {

            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/customsearch/v1")
                            .queryParam("key", apiKey)
                            .queryParam("cx", cx)
                            .queryParam("searchType", "image")
                            .queryParam("num", 1)
                            .queryParam("q", query)
                            .build()
                    )
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) return null;

            Object itemsObj = response.get("items");
            if (!(itemsObj instanceof List<?> items)) return null;

            if (items.isEmpty()) return null;

            Map first = (Map) items.get(0);

            return first.get("link").toString();

        } catch (Exception e) {
            return null;
        }
    }
}
