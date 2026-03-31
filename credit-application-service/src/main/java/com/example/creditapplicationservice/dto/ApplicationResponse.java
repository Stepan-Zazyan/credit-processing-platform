package com.example.creditapplicationservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class ApplicationResponse {
    private UUID id;
    private String clientName;
    private BigDecimal amount;
    private String status;

    public ApplicationResponse(UUID id, String clientName, BigDecimal amount, String status) {
        this.id = id;
        this.clientName = clientName;
        this.amount = amount;
        this.status = status;
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
}
