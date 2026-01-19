package com.guessme.guessme.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
public class ImageSearchService {

    private static final List<String> ALLOWED_DOMAINS = List.of(
            "wikipedia.org",
            "wikimedia.org",
            "fandom.com",
            "imdb.com"
    );

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final String apiKey;
    private final String cx;
    private final WebClient googleWebClient;

    public ImageSearchService(
            @Value("${google.api.key:}") String apiKey,
            @Value("${google.search.cx:}") String cx,
            WebClient.Builder webClientBuilder
    ) {
        this.apiKey = apiKey;
        this.cx = cx;
        this.googleWebClient = webClientBuilder
                .baseUrl("https://www.googleapis.com")
                .build();
    }

    public Mono<String> searchImage(String query) {
        if (query == null || query.isBlank()) return Mono.just("");
        if (apiKey == null || apiKey.isBlank()) return Mono.just("");
        if (cx == null || cx.isBlank()) return Mono.just("");

        String safeQuery = (query + " official character portrait").trim();

        Mono<String> fromAllowedDomains =
                Flux.fromIterable(ALLOWED_DOMAINS)
                        .concatMap(domain ->
                                searchInDomain(safeQuery, domain)
                                        .filter(url -> isAcceptable(url, domain))
                                        .onErrorResume(e -> Mono.empty())
                        )
                        .next();

        Mono<String> fromAnyDomain =
                searchInDomain(safeQuery, null)
                        .filter(url -> isAcceptable(url, null))
                        .onErrorResume(e -> Mono.empty());

        return fromAllowedDomains
                .switchIfEmpty(fromAnyDomain)
                .defaultIfEmpty("");
    }

    private Mono<String> searchInDomain(String q, String domain) {
        return googleWebClient.get()
                .uri(uriBuilder -> {
                    var b = uriBuilder
                            .path("/customsearch/v1")
                            .queryParam("key", apiKey)
                            .queryParam("cx", cx)
                            .queryParam("searchType", "image")
                            .queryParam("safe", "active")
                            .queryParam("num", 1)
                            .queryParam("q", q);

                    if (domain != null && !domain.isBlank()) {
                        b = b.queryParam("siteSearch", domain);
                    }
                    return b.build();
                })
                .retrieve()
                .bodyToMono(MAP_TYPE)
                .map(this::extractFirstImageLink)
                .defaultIfEmpty("");
    }

    @SuppressWarnings("unchecked")
    private String extractFirstImageLink(Map<String, Object> response) {
        if (response == null) return "";
        Object itemsObj = response.get("items");
        if (!(itemsObj instanceof List<?> items) || items.isEmpty()) return "";

        Object firstObj = items.getFirst();
        if (!(firstObj instanceof Map<?, ?> first)) return "";

        Object linkObj = first.get("link");
        return linkObj == null ? "" : linkObj.toString();
    }

    private boolean isAcceptable(String url, String expectedDomain) {
        if (url == null || url.isBlank()) return false;
        if (!url.startsWith("https://")) return false;

        if (expectedDomain != null && !expectedDomain.isBlank()) {
            if (!url.toLowerCase().contains(expectedDomain)) return false;
        }

        try {
            URI uri = URI.create(url);
            return uri.getHost() != null && !uri.getHost().isBlank();
        } catch (Exception ignored) {
            return false;
        }
    }
}
