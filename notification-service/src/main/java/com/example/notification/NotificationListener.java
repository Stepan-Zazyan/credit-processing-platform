package com.example.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {
    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    @KafkaListener(topics = "scoring-results", groupId = "notification-service")
    public void onMessage(ScoringResultEvent event, Acknowledgment acknowledgment) {
        log.info("Received scoring result: id={}, clientName={}, decision={}",
                event.applicationId(), event.clientName(), event.decision());

        if (event.clientName() != null && event.clientName().toLowerCase().contains("fail")) {
            log.warn("Triggering demo failure for notification id={} due to clientName={}",
                    event.applicationId(), event.clientName());
            throw new IllegalStateException("Demo failure for DLQ scenario");
        }

        log.info("Sending notification for application id={} with decision={}",
                event.applicationId(), event.decision());
        acknowledgment.acknowledge();
    }
}
