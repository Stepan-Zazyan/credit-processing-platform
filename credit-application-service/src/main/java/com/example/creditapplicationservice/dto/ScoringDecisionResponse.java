package com.example.creditapplicationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ScoringDecisionResponse {
    private String decision;
    private boolean fallback;

    public static ScoringDecisionResponse fallback() {
        return new ScoringDecisionResponse("REVIEW", true);
    }
}
