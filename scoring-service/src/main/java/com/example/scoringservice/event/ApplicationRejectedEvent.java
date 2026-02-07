package com.example.scoringservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event published when a credit application is rejected.
 */
public class ApplicationRejectedEvent {
    private UUID id;
    private String clientName;
    private BigDecimal amount;
    private String status;
    private String reason;
    private Instant scoredAt;

    public ApplicationRejectedEvent() {
    }

    public ApplicationRejectedEvent(UUID id, String clientName, BigDecimal amount, String status, String reason, Instant scoredAt) {
        this.id = id;
        this.clientName = clientName;
        this.amount = amount;
        this.status = status;
        this.reason = reason;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getScoredAt() {
        return scoredAt;
    }

    public void setScoredAt(Instant scoredAt) {
        this.scoredAt = scoredAt;
    }
}
