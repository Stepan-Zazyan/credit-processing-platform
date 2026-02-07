package com.example.scoring;

import java.math.BigDecimal;

public record ScoringResultEvent(
        String applicationId,
        String clientName,
        BigDecimal amount,
        String decision
) {
}
