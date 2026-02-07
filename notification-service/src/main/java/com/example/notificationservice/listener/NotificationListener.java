package com.example.notificationservice.listener;

import com.example.notificationservice.dto.ApplicationDecisionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {
    private static final Logger logger = LoggerFactory.getLogger(NotificationListener.class);

    @KafkaListener(
            topics = {"credit.application.approved", "credit.application.rejected"},
            groupId = "notification-service"
    )
    public void onDecision(ApplicationDecisionEvent event, Acknowledgment acknowledgment) {
        logger.info("Notification sent for application {} with status {}", event.getId(), event.getStatus());
        acknowledgment.acknowledge();
    }
}
