package com.example.creditapplicationservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.creditapplicationservice.dto.ScoringDecisionResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
        }
)
class ScoringClientResilienceTest {

    private static MockWebServer mockWebServer;

    @Autowired
    private ScoringClient scoringClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) throws IOException {
        if (mockWebServer == null) {
            mockWebServer = new MockWebServer();
            mockWebServer.start();
        }
        registry.add("downstream.scoring.base-url", () -> mockWebServer.url("/").toString());
    }

    @BeforeAll
    static void setupServer() throws IOException {
        if (mockWebServer == null) {
            mockWebServer = new MockWebServer();
            mockWebServer.start();
        }
    }

    @AfterAll
    static void shutdownServer() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    void resetCircuitBreaker() {
        circuitBreakerRegistry.circuitBreaker("scoringClient").reset();
    }

    @Test
    void successReturnsDownstreamResponse() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"decision\":\"APPROVE\",\"fallback\":false}")
                .addHeader("Content-Type", "application/json"));

        ScoringDecisionResponse response = scoringClient.getDecision("Ivan").join();

        assertThat(response.decision()).isEqualTo("APPROVE");
        assertThat(response.fallback()).isFalse();
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void repeatedFailureOpensCircuit() {
        for (int i = 0; i < 4; i++) {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));
            ScoringDecisionResponse response = scoringClient.getDecision("Ivan").join();
            assertThat(response.fallback()).isTrue();
        }

        ScoringDecisionResponse openCircuitResponse = scoringClient.getDecision("Ivan").join();

        assertThat(openCircuitResponse.fallback()).isTrue();
        assertThat(mockWebServer.getRequestCount()).isEqualTo(4);
        assertThat(circuitBreakerRegistry.circuitBreaker("scoringClient").getState().name()).isEqualTo("OPEN");
    }

    @Test
    void retryHappensOnTransientFailure() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"decision\":\"APPROVE\",\"fallback\":false}")
                .addHeader("Content-Type", "application/json"));

        ScoringDecisionResponse response = scoringClient.getDecision("Ivan").join();

        assertThat(response.decision()).isEqualTo("APPROVE");
        assertThat(response.fallback()).isFalse();
        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
    }

    @Test
    void fallbackIsReturnedWhenDownstreamUnavailable() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBodyDelay(2, java.util.concurrent.TimeUnit.SECONDS)
                .setBody("{\"decision\":\"APPROVE\",\"fallback\":false}")
                .addHeader("Content-Type", "application/json"));

        ScoringDecisionResponse response = scoringClient.getDecision("Ivan").join();

        assertThat(response).isEqualTo(ScoringDecisionResponse.fallback());
        assertThat(mockWebServer.getRequestCount()).isGreaterThanOrEqualTo(1);
    }
}
