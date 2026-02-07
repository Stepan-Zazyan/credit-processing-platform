package com.example.scoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class ScoringListener {
    private static final Logger log = LoggerFactory.getLogger(ScoringListener.class);
    private final KafkaTemplate<String, ScoringResultEvent> kafkaTemplate;

    public ScoringListener(KafkaTemplate<String, ScoringResultEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "credit-applications", groupId = "scoring-service")
    public void onMessage(CreditApplicationEvent event, Acknowledgment acknowledgment) {
        log.info("Received credit application: id={}, clientName={}, amount={}",
                event.applicationId(), event.clientName(), event.amount());

        if (event.clientName() != null && event.clientName().toLowerCase().contains("fail")) {
            log.warn("Triggering demo failure for application id={} due to clientName={}",
                    event.applicationId(), event.clientName());
            throw new IllegalStateException("Demo failure for DLQ scenario");
        }

        String decision = event.amount().doubleValue() <= 10000 ? "APPROVE" : "REJECT";
        log.info("Decision for application id={} is {}", event.applicationId(), decision);

        ScoringResultEvent result = new ScoringResultEvent(
                event.applicationId(),
                event.clientName(),
                event.amount(),
                decision
        );
        kafkaTemplate.send("scoring-results", event.applicationId(), result);
        log.info("Published scoring result event: id={}, decision={}", event.applicationId(), decision);
        acknowledgment.acknowledge();
    }
}
