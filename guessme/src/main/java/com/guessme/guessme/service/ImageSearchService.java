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
            String url = "/customsearch/v1"
                    + "?key=" + apiKey
                    + "&cx=" + cx
                    + "&searchType=image"
                    + "&num=1"
                    + "&q=" + query;

            Map response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || !response.containsKey("items")) {
                return null;
            }

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

            if (items.isEmpty()) return null;

            return items.get(0).get("link").toString();

        } catch (Exception e) {
            return null;
        }
    }
}
