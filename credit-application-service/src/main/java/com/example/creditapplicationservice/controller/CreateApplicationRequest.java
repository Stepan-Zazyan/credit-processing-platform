package com.example.creditapplicationservice.controller;

import java.math.BigDecimal;

public class CreateApplicationRequest {
    private String clientName;
    private BigDecimal amount;

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
