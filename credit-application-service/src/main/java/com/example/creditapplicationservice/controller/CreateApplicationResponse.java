package com.example.creditapplicationservice.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class CreateApplicationResponse {
    private UUID id;
    private String clientName;
    private BigDecimal amount;
    private String status;
    private Instant createdAt;

    public CreateApplicationResponse(UUID id, String clientName, BigDecimal amount, String status, Instant createdAt) {
        this.id = id;
        this.clientName = clientName;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getClientName() {
        return clientName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
