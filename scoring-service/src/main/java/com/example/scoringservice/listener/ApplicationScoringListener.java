package com.example.scoringservice.listener;

import com.example.scoringservice.dto.ApplicationCreatedEvent;
import com.example.scoringservice.dto.ApplicationDecisionEvent;
import com.example.scoringservice.service.IdempotentEventProcessor;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ApplicationScoringListener {
    private static final String APPROVED_TOPIC = "credit.application.approved";
    private static final String REJECTED_TOPIC = "credit.application.rejected";
    private static final BigDecimal APPROVAL_THRESHOLD = new BigDecimal("10000.00");

    private final KafkaTemplate<String, ApplicationDecisionEvent> kafkaTemplate;
    private final IdempotentEventProcessor idempotentEventProcessor;

    public ApplicationScoringListener(
            KafkaTemplate<String, ApplicationDecisionEvent> kafkaTemplate,
            IdempotentEventProcessor idempotentEventProcessor
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.idempotentEventProcessor = idempotentEventProcessor;
    }

    @KafkaListener(topics = "credit.application.created", groupId = "scoring-service")
    public void onMessage(ApplicationCreatedEvent event, Acknowledgment acknowledgment) {
        String eventId = event.getId().toString();
        idempotentEventProcessor.process(eventId, () -> {
            String decision = evaluate(event.getAmount());
            String topic = "APPROVED".equals(decision) ? APPROVED_TOPIC : REJECTED_TOPIC;
            ApplicationDecisionEvent decisionEvent = new ApplicationDecisionEvent(event.getApplicationId(), decision);
            kafkaTemplate.send(topic, event.getApplicationId().toString(), decisionEvent);
            log.info("Scoring finished for application {} with decision {}", event.getApplicationId(), decision);
        });
        acknowledgment.acknowledge();
    }

    private String evaluate(BigDecimal amount) {
        if (amount == null) {
            return "REJECTED";
        }
        return amount.compareTo(APPROVAL_THRESHOLD) <= 0 ? "APPROVED" : "REJECTED";
    }
}
