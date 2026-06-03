package com.guessme.guessme;

import com.guessme.guessme.config.CorsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {

    private CorsWebFilter buildFilter(String origins) {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins", origins);
        return config.corsWebFilter();
    }

    private CorsConfiguration resolveConfig(CorsWebFilter filter) {
        CorsConfigurationSource source = (CorsConfigurationSource)
                ReflectionTestUtils.getField(filter, "configSource");
        assertNotNull(source, "CorsWebFilter must expose a configSource field");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/game/categories").build()
        );
        return source.getCorsConfiguration(exchange);
    }

    // ── origin parsing ─────────────────────────────────────────────────────

    @Test
    void corsWebFilter_singleOrigin_allowedOriginsContainIt() {
        CorsConfiguration cors = resolveConfig(buildFilter("http://localhost:5173"));

        assertNotNull(cors);
        List<String> origins = cors.getAllowedOrigins();
        assertNotNull(origins);
        assertTrue(origins.contains("http://localhost:5173"));
    }

    @Test
    void corsWebFilter_commaListWithSpaces_allOriginsRegistered() {
        CorsConfiguration cors = resolveConfig(
                buildFilter("http://localhost:5173 , https://example.com"));

        assertNotNull(cors);
        List<String> origins = cors.getAllowedOrigins();
        assertNotNull(origins);
        assertTrue(origins.contains("http://localhost:5173"),
                "first origin must be present");
        assertTrue(origins.contains("https://example.com"),
                "second origin must be present after trimming");
        assertEquals(2, origins.size(), "no extra origins should be registered");
    }

    // ── method / header / credentials policy ───────────────────────────────

    @Test
    void corsWebFilter_allowedMethodsAndHeaders_areWildcard() {
        CorsConfiguration cors = resolveConfig(buildFilter("http://localhost:5173"));

        assertNotNull(cors);
        assertTrue(cors.getAllowedMethods().contains("*"),
                "CorsConfig must allow all methods");
        assertTrue(cors.getAllowedHeaders().contains("*"),
                "CorsConfig must allow all headers");
    }

    @Test
    void corsWebFilter_allowCredentials_isNotTrue() {
        CorsConfiguration cors = resolveConfig(buildFilter("http://localhost:5173"));

        assertNotNull(cors);
        assertNotEquals(Boolean.TRUE, cors.getAllowCredentials(),
                "credentials must not be explicitly allowed");
    }
}
