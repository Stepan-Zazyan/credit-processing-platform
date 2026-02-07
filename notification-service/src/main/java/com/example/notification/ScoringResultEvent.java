package com.example.notification;

import java.math.BigDecimal;

public record ScoringResultEvent(
        String applicationId,
        String clientName,
        BigDecimal amount,
        String decision
) {
}
