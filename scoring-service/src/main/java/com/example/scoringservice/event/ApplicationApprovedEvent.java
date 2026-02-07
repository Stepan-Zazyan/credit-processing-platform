package com.example.scoringservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event published when a credit application is approved.
 */
public class ApplicationApprovedEvent {
    private UUID id;
    private String clientName;
    private BigDecimal amount;
    private String status;
    private Instant scoredAt;

    public ApplicationApprovedEvent() {
    }

    public ApplicationApprovedEvent(UUID id, String clientName, BigDecimal amount, String status, Instant scoredAt) {
        this.id = id;
        this.clientName = clientName;
        this.amount = amount;
        this.status = status;
        this.scoredAt = scoredAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getScoredAt() {
        return scoredAt;
    }

    public void setScoredAt(Instant scoredAt) {
        this.scoredAt = scoredAt;
    }
}
