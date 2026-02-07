package com.example.creditapplicationservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class ApplicationCreatedEvent {
    private UUID id;
    private String clientName;
    private BigDecimal amount;
    private String status;

    public ApplicationCreatedEvent() {
    }

    public ApplicationCreatedEvent(UUID id, String clientName, BigDecimal amount, String status) {
        this.id = id;
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
