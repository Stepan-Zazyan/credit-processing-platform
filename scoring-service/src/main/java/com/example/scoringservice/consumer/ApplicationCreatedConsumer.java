package com.example.scoringservice.consumer;

import com.example.scoringservice.event.ApplicationCreatedEvent;
import com.example.scoringservice.service.ScoringService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for newly created credit applications.
 */
@Component
public class ApplicationCreatedConsumer {

    private final ScoringService scoringService;

    public ApplicationCreatedConsumer(ScoringService scoringService) {
        this.scoringService = scoringService;
    }

    @KafkaListener(topics = "credit.application.created", groupId = "scoring-service")
    public void onMessage(ConsumerRecord<String, ApplicationCreatedEvent> record, Acknowledgment acknowledgment) {
        scoringService.score(record.value());
        acknowledgment.acknowledge();
    }
}
