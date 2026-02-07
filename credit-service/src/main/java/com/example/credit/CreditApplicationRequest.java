package com.example.credit;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreditApplicationRequest(
        @NotBlank String clientName,
        @NotNull @DecimalMin("0.01") BigDecimal amount
) {
}
