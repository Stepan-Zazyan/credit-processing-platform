package com.example.creditapplicationservice.service;

import com.example.creditapplicationservice.dto.ScoringDecisionResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ScoringClient {
    private static final Logger log = LoggerFactory.getLogger(ScoringClient.class);
    private static final String RESILIENCE_INSTANCE = "scoringClient";

    private final RestClient restClient;

    public ScoringClient(@Qualifier("scoringRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "fallback")
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "fallback")
    @TimeLimiter(name = RESILIENCE_INSTANCE, fallbackMethod = "fallback")
    public CompletableFuture<ScoringDecisionResponse> getDecision(String clientName) {
        return CompletableFuture.supplyAsync(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/scoring/decision")
                        .queryParam("clientName", clientName)
                        .build())
                .retrieve()
                .body(ScoringDecisionResponse.class));
    }

    @SuppressWarnings("unused")
    private CompletableFuture<ScoringDecisionResponse> fallback(String clientName, Throwable throwable) {
        log.warn("Returning fallback decision for clientName={}", clientName, throwable);
        return CompletableFuture.completedFuture(ScoringDecisionResponse.fallback());
    }
}
