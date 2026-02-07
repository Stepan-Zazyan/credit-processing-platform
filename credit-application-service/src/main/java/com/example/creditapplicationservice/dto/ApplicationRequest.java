package com.example.creditapplicationservice.dto;

import java.math.BigDecimal;

public class ApplicationRequest {
    private String clientName;
    private BigDecimal amount;

    public ApplicationRequest() {
    }

    public ApplicationRequest(String clientName, BigDecimal amount) {
        this.clientName = clientName;
        this.amount = amount;
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
}
