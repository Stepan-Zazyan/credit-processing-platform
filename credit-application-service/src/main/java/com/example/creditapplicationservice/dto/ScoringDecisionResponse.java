package com.example.creditapplicationservice.dto;

public record ScoringDecisionResponse(String decision, boolean fallback) {
    public static ScoringDecisionResponse fallback() {
        return new ScoringDecisionResponse("REVIEW", true);
    }
}
