package com.example.springboot_api.services.shared.ai;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.example.springboot_api.dto.shared.ai.GoogleSearchResponse;
import com.example.springboot_api.dto.shared.ai.WebSearchItem;
import com.example.springboot_api.dto.shared.ai.WebSearchResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class WebSearchService {

    private static final Logger log = LoggerFactory.getLogger(WebSearchService.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${google.api.search_key}")
    private String apiKey;

    @Value("${google.api.cx}")
    private String cx;

    public WebSearchService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public WebSearchResult search(String query) {
        long start = System.currentTimeMillis();

        String rawJson;
        try {
            rawJson = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("www.googleapis.com")
                            .path("/customsearch/v1")
                            .queryParam("key", apiKey)
                            .queryParam("cx", cx)
                            .queryParam("q", query)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            clientResponse -> clientResponse.createException().map(ex -> new RuntimeException(
                                    "Google Search API error: " + ex.getMessage())))
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException ex) {
            log.error("Google Search HTTP error: status={} body={}", ex.getStatusCode(),
                    ex.getResponseBodyAsString());
            throw new RuntimeException(
                    "Error calling Google Search API: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString(),
                    ex);
        } catch (Exception ex) {
            log.error("Error calling Google Search API", ex);
            throw new RuntimeException("Error calling Google Search API", ex);
        }

        long elapsed = System.currentTimeMillis() - start;

        if (rawJson == null || rawJson.isEmpty()) {
            log.warn("Empty response from Google Search API");
            return new WebSearchResult(query, elapsed, Collections.emptyList());
        }

        GoogleSearchResponse googleResponse;
        try {
            googleResponse = objectMapper.readValue(rawJson, GoogleSearchResponse.class);
        } catch (JsonProcessingException e) {
            // Log raw JSON để debug khi cần
            log.error("Failed to parse Google Search JSON. rawJson={}", rawJson, e);
            throw new RuntimeException("Failed to parse Google Search response", e);
        }

        if (googleResponse.getItems() == null) {
            return new WebSearchResult(query, elapsed, Collections.emptyList());
        }

        List<WebSearchItem> items = googleResponse.getItems().stream()
                .map(i -> new WebSearchItem(
                        i.getTitle(),
                        i.getLink(),
                        i.getSnippet()))
                .toList();

        long searchTimeMs = elapsed;
        if (googleResponse.getSearchInformation() != null &&
                googleResponse.getSearchInformation().getSearchTime() != null) {
            searchTimeMs = Math.round(googleResponse.getSearchInformation().getSearchTime() * 1000);
        }

        return new WebSearchResult(query, searchTimeMs, items);
    }
}
