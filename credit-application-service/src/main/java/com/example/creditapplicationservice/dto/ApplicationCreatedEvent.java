package com.example.creditapplicationservice.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationCreatedEvent {
    private UUID eventId;
    private UUID applicationId;
    private String clientName;
    private BigDecimal amount;
    private String status;
}
