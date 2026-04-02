package com.example.scoringservice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationCreatedEvent {
    @JsonAlias("eventId")
    private UUID id;

    @JsonAlias("applicationId")
    private UUID applicationId;

    private String clientName;
    private BigDecimal amount;
    private String status;
}
