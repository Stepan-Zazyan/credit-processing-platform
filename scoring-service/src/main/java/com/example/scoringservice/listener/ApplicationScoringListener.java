package com.example.scoringservice.listener;

import com.example.scoringservice.dto.ApplicationCreatedEvent;
import com.example.scoringservice.dto.ApplicationDecisionEvent;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class ApplicationScoringListener {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationScoringListener.class);
    private static final String APPROVED_TOPIC = "credit.application.approved";
    private static final String REJECTED_TOPIC = "credit.application.rejected";
    private static final BigDecimal APPROVAL_THRESHOLD = new BigDecimal("10000.00");

    private final KafkaTemplate<String, ApplicationDecisionEvent> kafkaTemplate;

    public ApplicationScoringListener(KafkaTemplate<String, ApplicationDecisionEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "credit.application.created", groupId = "scoring-service")
    public void onMessage(ApplicationCreatedEvent event, Acknowledgment acknowledgment) {
        String decision = evaluate(event.getAmount());
        String topic = "APPROVED".equals(decision) ? APPROVED_TOPIC : REJECTED_TOPIC;
        ApplicationDecisionEvent decisionEvent = new ApplicationDecisionEvent(event.getId(), decision);
        kafkaTemplate.send(topic, event.getId().toString(), decisionEvent);
        logger.info("Scoring finished for application {} with decision {}", event.getId(), decision);
        acknowledgment.acknowledge();
    }

    private String evaluate(BigDecimal amount) {
        if (amount == null) {
            return "REJECTED";
        }
        return amount.compareTo(APPROVAL_THRESHOLD) <= 0 ? "APPROVED" : "REJECTED";
    }
}
