package com.example.notificationservice.consumer;

import com.example.notificationservice.event.ApplicationApprovedEvent;
import com.example.notificationservice.event.ApplicationRejectedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that simulates notification delivery for approved and rejected applications.
 */
@Component
public class NotificationConsumer {

    private static final Logger logger = LoggerFactory.getLogger(NotificationConsumer.class);

    @KafkaListener(topics = "credit.application.approved", groupId = "notification-service")
    public void onApproved(ConsumerRecord<String, ApplicationApprovedEvent> record, Acknowledgment acknowledgment) {
        logger.info("Notification sent for application {}", record.value().getId());
        acknowledgment.acknowledge();
    }

    @KafkaListener(topics = "credit.application.rejected", groupId = "notification-service")
    public void onRejected(ConsumerRecord<String, ApplicationRejectedEvent> record, Acknowledgment acknowledgment) {
        logger.info("Notification sent for application {}", record.value().getId());
        acknowledgment.acknowledge();
    }
}
