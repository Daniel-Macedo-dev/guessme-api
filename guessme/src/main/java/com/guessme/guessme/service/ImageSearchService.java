package com.guessme.guessme.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
public class ImageSearchService {

    private final String apiKey;
    private final String cx;
    private final WebClient webClient;

    private static final List<String> ALLOWED_DOMAINS = List.of(
            "wikipedia.org",
            "wikimedia.org",
            "fandom.com",
            "imdb.com"
    );

    public ImageSearchService(
            @Value("${google.api.key}") String apiKey,
            @Value("${google.search.cx}") String cx,
            WebClient.Builder webClientBuilder
    ) {
        this.apiKey = apiKey;
        this.cx = cx;
        this.webClient = webClientBuilder
                .baseUrl("https://www.googleapis.com")
                .build();
    }

    public String searchImage(String query) {
        if (query == null || query.isBlank()) return null;
        if (apiKey == null || apiKey.isBlank()) return null;
        if (cx == null || cx.isBlank()) return null;

        // aqui a query já chega “limpa” do GameService
        final String q = query.trim();

        for (String domain : ALLOWED_DOMAINS) {
            String link = searchInDomain(q, domain);
            if (isAcceptable(link, domain)) return link;
        }

        String link = searchInDomain(q, null);
        if (isAcceptable(link, null)) return link;

        return null;
    }

    private String searchInDomain(String q, String domain) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> {
                        var b = uriBuilder
                                .path("/customsearch/v1")
                                .queryParam("key", apiKey)
                                .queryParam("cx", cx)
                                .queryParam("searchType", "image")
                                .queryParam("safe", "active")
                                .queryParam("num", 1)
                                .queryParam("q", q);

                        if (domain != null) {
                            b = b.queryParam("siteSearch", domain);
                        }

                        return b.build();
                    })
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) return null;

            Object itemsObj = response.get("items");
            if (!(itemsObj instanceof List<?> items) || items.isEmpty()) return null;

            Map<?, ?> first = (Map<?, ?>) items.get(0);
            Object linkObj = first.get("link");
            return linkObj != null ? linkObj.toString() : null;

        } catch (Exception e) {
            return null;
        }
    }

    private boolean isAcceptable(String url, String expectedDomain) {
        if (url == null || url.isBlank()) return false;
        if (!url.startsWith("https://")) return false;

        if (expectedDomain != null && !url.toLowerCase().contains(expectedDomain)) return false;

        try {
            URI.create(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
