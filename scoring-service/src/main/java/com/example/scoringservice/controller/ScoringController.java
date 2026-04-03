package com.example.scoringservice.controller;

import com.example.scoringservice.dto.ScoringDecisionResponse;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/scoring")
public class ScoringController {

    @GetMapping("/decision")
    public ScoringDecisionResponse getDecision(@RequestParam("clientName") String clientName) {
        String normalized = clientName == null ? "" : clientName.toLowerCase(Locale.ROOT);
        String decision = normalized.length() % 2 == 0 ? "APPROVE" : "REVIEW";
        return new ScoringDecisionResponse(decision, false);
    }
}
