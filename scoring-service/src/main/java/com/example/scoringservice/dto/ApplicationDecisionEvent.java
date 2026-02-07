package com.example.scoringservice.dto;

import java.util.UUID;

public class ApplicationDecisionEvent {
    private UUID id;
    private String status;

    public ApplicationDecisionEvent() {
    }

    public ApplicationDecisionEvent(UUID id, String status) {
        this.id = id;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
