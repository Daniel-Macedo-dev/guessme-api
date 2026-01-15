package com.guessme.guessme.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
public class ImageSearchService {

    private final WebClient webClient;
    private final String apiKey;
    private final String cx;

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private static final List<String> TRUSTED_DOMAINS = List.of(
            "wikipedia.org",
            "wikimedia.org",
            "fandom.com",
            "imdb.com"
    );

    public ImageSearchService(
            WebClient.Builder webClientBuilder,
            @Value("${google.api.key}") String apiKey,
            @Value("${google.search.cx}") String cx
    ) {
        this.webClient = webClientBuilder
                .baseUrl("https://www.googleapis.com")
                .build();
        this.apiKey = apiKey;
        this.cx = cx;
    }

    public String searchImage(String baseQuery) {

        List<String> queries = List.of(
                baseQuery + " official character portrait",
                baseQuery + " character",
                baseQuery + " movie character",
                baseQuery
        );

        // 1) Domínios confiáveis primeiro
        for (String query : queries) {
            for (String domain : TRUSTED_DOMAINS) {
                String link = search(query, domain);
                if (isValid(link, domain)) return link;
            }
        }

        // 2) Fallback geral
        for (String query : queries) {
            String link = search(query, null);
            if (isValid(link, null)) return link;
        }

        return null;
    }

    private String search(String query, String domain) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path("/customsearch/v1")
                                .queryParam("key", apiKey)
                                .queryParam("cx", cx)
                                .queryParam("searchType", "image")
                                .queryParam("safe", "active")
                                .queryParam("num", 5)
                                .queryParam("q", query);

                        if (domain != null) {
                            builder = builder.queryParam("siteSearch", domain);
                        }

                        return builder.build();
                    })
                    .retrieve()
                    .bodyToMono(MAP_TYPE)
                    .block();

            if (response == null) return null;

            Object itemsObj = response.get("items");
            if (!(itemsObj instanceof List<?> items) || items.isEmpty()) return null;

            for (Object obj : items) {
                if (!(obj instanceof Map<?, ?> item)) continue;

                Object linkObj = item.get("link");
                if (linkObj == null) continue;

                String link = linkObj.toString();
                if (isValid(link, domain)) return link;
            }

        } catch (Exception ignored) {
        }

        return null;
    }

    private boolean isValid(String url, String expectedDomain) {
        if (url == null || url.isBlank()) return false;
        if (!url.startsWith("https://")) return false;

        if (expectedDomain != null && !url.toLowerCase().contains(expectedDomain)) {
            return false;
        }

        try {
            URI uri = URI.create(url);
            return uri.getScheme() != null && uri.getHost() != null;
        } catch (Exception e) {
            return false;
        }
    }
}
