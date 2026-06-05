package com.guessme.guessme;

import com.guessme.guessme.service.ImageSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class ImageSearchServiceTest {

    @Mock private WebClient.Builder webClientBuilder;
    @Mock private WebClient googleWebClient;
    @Mock private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock private WebClient.ResponseSpec responseSpec;

    private static final String VALID_KEY = "test-api-key";
    private static final String VALID_CX  = "test-cx";

    @BeforeEach
    void setUp() {
        // Always consumed by the constructor's baseUrl().build() chain.
        when(webClientBuilder.baseUrl(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(googleWebClient);

        // WebClient call chain — only reached in behavior tests.
        // lenient() prevents strict-stubs failures in guard-clause tests.
        lenient().when(googleWebClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    private ImageSearchService service(String apiKey, String cx) {
        return new ImageSearchService(apiKey, cx, webClientBuilder);
    }

    // ── guard clauses ──────────────────────────────────────────────────────

    @Test
    void searchImage_nullQuery_returnsEmpty() {
        String result = service(VALID_KEY, VALID_CX).searchImage(null).block();
        assertEquals("", result);
    }

    @Test
    void searchImage_blankQuery_returnsEmpty() {
        String result = service(VALID_KEY, VALID_CX).searchImage("   ").block();
        assertEquals("", result);
    }

    @Test
    void searchImage_blankApiKey_returnsEmpty() {
        String result = service("", VALID_CX).searchImage("Naruto character").block();
        assertEquals("", result);
    }

    @Test
    void searchImage_blankCx_returnsEmpty() {
        String result = service(VALID_KEY, "").searchImage("Naruto character").block();
        assertEquals("", result);
    }

    // ── response extraction and URL filtering ──────────────────────────────

    @Test
    void searchImage_emptyItemsInResponse_returnsEmpty() {
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.just(Map.of("items", List.of())));

        String result = service(VALID_KEY, VALID_CX).searchImage("Naruto").block();

        assertEquals("", result);
    }

    @Test
    void searchImage_validHttpsUrlFromAllowedDomain_returnsUrl() {
        // URL belongs to wikipedia.org (first allowed domain) — accepted on first try.
        String expectedUrl = "https://en.wikipedia.org/wiki/Naruto.jpg";
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.just(Map.of("items", List.of(Map.of("link", expectedUrl)))));

        String result = service(VALID_KEY, VALID_CX).searchImage("Naruto").block();

        assertEquals(expectedUrl, result);
        // The any-domain fallback must never be assembled when an allowed domain succeeds.
        // With Mono.defer the fallback is lazy, so bodyToMono is called exactly once.
        verify(responseSpec, times(1)).bodyToMono(any(ParameterizedTypeReference.class));
    }

    @Test
    void searchImage_httpUrlRejected_returnsEmpty() {
        // isAcceptable rejects non-HTTPS URLs; all searches return the same bad URL.
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.just(Map.of("items", List.of(Map.of("link", "http://example.com/img.jpg")))));

        String result = service(VALID_KEY, VALID_CX).searchImage("Naruto").block();

        assertEquals("", result);
    }

    // ── fallback chain ─────────────────────────────────────────────────────

    @Test
    void searchImage_allDomainsFail_fallsBackToAnyDomain() {
        // fromAnyDomain is wrapped in Mono.defer, so searchInDomain(null) is only called
        // after all allowed-domain searches complete empty.
        // ALLOWED_DOMAINS order: wikipedia.org, wikimedia.org, fandom.com, imdb.com.
        Map<String, Object> emptyItems = Map.of("items", List.of());
        String expectedUrl = "https://static.wikia.nocookie.net/naruto.jpg";
        Map<String, Object> goodResponse = Map.of("items", List.of(Map.of("link", expectedUrl)));

        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.just(emptyItems))   // call 1: wikipedia.org
            .thenReturn(Mono.just(emptyItems))   // call 2: wikimedia.org
            .thenReturn(Mono.just(emptyItems))   // call 3: fandom.com
            .thenReturn(Mono.just(emptyItems))   // call 4: imdb.com
            .thenReturn(Mono.just(goodResponse)); // call 5: any-domain fallback (lazy)

        String result = service(VALID_KEY, VALID_CX).searchImage("Naruto").block();

        assertEquals(expectedUrl, result);
        verify(responseSpec, times(5)).bodyToMono(any(ParameterizedTypeReference.class));
    }
}
