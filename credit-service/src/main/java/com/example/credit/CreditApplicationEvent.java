package com.example.credit;

import java.math.BigDecimal;

public record CreditApplicationEvent(
        String applicationId,
        String clientName,
        BigDecimal amount
) {
}
