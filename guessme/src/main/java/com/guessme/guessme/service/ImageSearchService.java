package com.guessme.guessme.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ImageSearchService {

    @Value("${google.api.key}")
    private String apiKey;

    @Value("${google.search.cx}")
    private String cx;

    private final WebClient webClient = WebClient.create("https://www.googleapis.com");

    private static final List<String> ALLOWED_DOMAINS = List.of(
            "wikipedia.org",
            "wikimedia.org",
            "fandom.com",
            "imdb.com"
    );

    public String searchImage(String query) {
        String safeQuery = query + " official character portrait";

        for (String domain : ALLOWED_DOMAINS) {
            String link = searchInDomain(safeQuery, domain);
            if (isAcceptable(link, domain)) return link;
        }

        String link = searchInDomain(safeQuery, null);
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
