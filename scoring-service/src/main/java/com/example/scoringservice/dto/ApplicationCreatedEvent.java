package com.example.scoringservice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.math.BigDecimal;
import java.util.UUID;

public class ApplicationCreatedEvent {
    @JsonAlias("eventId")
    private UUID id;

    @JsonAlias("applicationId")
    private UUID applicationId;

    private String clientName;
    private BigDecimal amount;
    private String status;

    public ApplicationCreatedEvent() {
    }

    public ApplicationCreatedEvent(UUID id, UUID applicationId, String clientName, BigDecimal amount, String status) {
        this.id = id;
        this.applicationId = applicationId;
        this.clientName = clientName;
        this.amount = amount;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
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
}
