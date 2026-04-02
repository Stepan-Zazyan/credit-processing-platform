package com.example.creditapplicationservice.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApplicationResponse {
    private UUID id;
    private String clientName;
    private BigDecimal amount;
    private String status;
}
