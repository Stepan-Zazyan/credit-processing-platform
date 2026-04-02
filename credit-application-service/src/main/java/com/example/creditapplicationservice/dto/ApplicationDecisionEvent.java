package com.example.creditapplicationservice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDecisionEvent {
    private UUID eventId;

    @JsonAlias("id")
    private UUID applicationId;

    @JsonAlias("status")
    private String decision;
    private String reason;
}
