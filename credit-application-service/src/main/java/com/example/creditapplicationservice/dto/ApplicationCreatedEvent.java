package com.example.creditapplicationservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class ApplicationCreatedEvent {
    private UUID eventId;
    private UUID applicationId;
    private String clientName;
    private BigDecimal amount;
    private String status;

    public ApplicationCreatedEvent() {
    }

    public ApplicationCreatedEvent(UUID eventId, UUID applicationId, String clientName, BigDecimal amount, String status) {
        this.eventId = eventId;
        this.applicationId = applicationId;
        this.clientName = clientName;
        this.amount = amount;
        this.status = status;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
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
