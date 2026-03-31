package com.example.creditapplicationservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class ApplicationRequest {
    @NotBlank
    private String clientName;

    @NotNull
    @DecimalMin(value = "0.01")
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
