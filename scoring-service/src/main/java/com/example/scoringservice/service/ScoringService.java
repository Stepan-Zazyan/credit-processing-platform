package com.example.scoringservice.service;

import com.example.scoringservice.event.ApplicationApprovedEvent;
import com.example.scoringservice.event.ApplicationCreatedEvent;
import com.example.scoringservice.event.ApplicationRejectedEvent;
import java.time.Instant;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Business logic for evaluating credit applications and publishing scoring results.
 */
@Service
public class ScoringService {

    public static final String APPROVED_TOPIC = "credit.application.approved";
    public static final String REJECTED_TOPIC = "credit.application.rejected";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ScoringService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void score(ApplicationCreatedEvent event) {
        if (event.getClientName() != null && event.getClientName().toLowerCase().contains("fail")) {
            throw new IllegalStateException("Demo failure for DLQ");
        }

        boolean approved = event.getAmount() != null && event.getAmount().doubleValue() <= 10000.0;
        if (approved) {
            ApplicationApprovedEvent approvedEvent = new ApplicationApprovedEvent(
                    event.getId(),
                    event.getClientName(),
                    event.getAmount(),
                    "APPROVED",
                    Instant.now()
            );
            kafkaTemplate.send(APPROVED_TOPIC, event.getId().toString(), approvedEvent);
        } else {
            ApplicationRejectedEvent rejectedEvent = new ApplicationRejectedEvent(
                    event.getId(),
                    event.getClientName(),
                    event.getAmount(),
                    "REJECTED",
                    "Amount above limit",
                    Instant.now()
            );
            kafkaTemplate.send(REJECTED_TOPIC, event.getId().toString(), rejectedEvent);
        }
    }
}
