package com.example.scoring;

import java.math.BigDecimal;

public record CreditApplicationEvent(
        String applicationId,
        String clientName,
        BigDecimal amount
) {
}
