package com.example.creditapplicationservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event published when a new credit application is created.
 */
public class ApplicationCreatedEvent {
    private UUID id;
    private String clientName;
    private BigDecimal amount;
    private String status;
    private Instant createdAt;

    public ApplicationCreatedEvent() {
    }

    public ApplicationCreatedEvent(UUID id, String clientName, BigDecimal amount, String status, Instant createdAt) {
        this.id = id;
        this.clientName = clientName;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
