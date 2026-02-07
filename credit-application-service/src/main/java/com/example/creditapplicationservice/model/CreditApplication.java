package com.example.creditapplicationservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "credit_application")
public class CreditApplication {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CreditApplication() {
    }

    public CreditApplication(UUID id, String clientName, BigDecimal amount, ApplicationStatus status, Instant createdAt) {
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

    public ApplicationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }
}
