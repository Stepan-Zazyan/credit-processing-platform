package com.example.scoringservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ScoringDecisionResponse {
    private String decision;
    private boolean fallback;
}
