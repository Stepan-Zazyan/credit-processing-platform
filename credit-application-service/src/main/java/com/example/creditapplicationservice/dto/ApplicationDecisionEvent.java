package com.example.creditapplicationservice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.UUID;

public class ApplicationDecisionEvent {
    private UUID eventId;

    @JsonAlias("id")
    private UUID applicationId;

    @JsonAlias("status")
    private String decision;
    private String reason;

    public ApplicationDecisionEvent() {
    }

    public ApplicationDecisionEvent(UUID eventId, UUID applicationId, String decision, String reason) {
        this.eventId = eventId;
        this.applicationId = applicationId;
        this.decision = decision;
        this.reason = reason;
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

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
