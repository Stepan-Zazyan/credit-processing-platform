package com.example.creditapplicationservice.service;

import com.example.creditapplicationservice.client.ScoringFeignClient;
import com.example.creditapplicationservice.dto.ScoringDecisionResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScoringClient {
    private static final String RESILIENCE_INSTANCE = "scoringClient";

    private final ScoringFeignClient scoringFeignClient;

    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "fallback")
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "fallback")
    public ScoringDecisionResponse getDecision(String clientName) {
        return scoringFeignClient.getDecision(clientName);
    }

    @SuppressWarnings("unused")
    private ScoringDecisionResponse fallback(String clientName, Throwable throwable) {
        log.warn("Returning fallback decision for clientName={}", clientName, throwable);
        return ScoringDecisionResponse.fallback();
    }
}
