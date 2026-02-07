package com.example.creditapplicationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "applications")
public class Application {
    @Id
    private UUID id;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Application() {
    }

    public Application(UUID id, String clientName, BigDecimal amount, String status) {
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
