package com.example.creditapplicationservice.service;

import com.example.creditapplicationservice.dto.ApplicationCreatedEvent;
import com.example.creditapplicationservice.entity.Application;
import com.example.creditapplicationservice.entity.OutboxEvent;
import com.example.creditapplicationservice.repository.ApplicationRepository;
import com.example.creditapplicationservice.repository.OutboxEventRepository;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxPublisherService {
    public static final String OUTBOX_STATUS_NEW = "NEW";
    public static final String OUTBOX_STATUS_PUBLISHED = "PUBLISHED";

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherService.class);

    private final ApplicationRepository applicationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, ApplicationCreatedEvent> kafkaTemplate;

    public OutboxPublisherService(ApplicationRepository applicationRepository,
                                 OutboxEventRepository outboxEventRepository,
                                 KafkaTemplate<String, ApplicationCreatedEvent> kafkaTemplate) {
        this.applicationRepository = applicationRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public boolean publishEvent(OutboxEvent outboxEvent) {
        Application application = applicationRepository.findById(outboxEvent.getAggregateId()).orElse(null);
        if (application == null) {
            log.warn("Outbox event {} references missing application {}", outboxEvent.getId(), outboxEvent.getAggregateId());
            return false;
        }

        try {
            ApplicationCreatedEvent event = new ApplicationCreatedEvent(
                    application.getId(),
                    application.getClientName(),
                    application.getAmount(),
                    application.getStatus()
            );

            kafkaTemplate.send(outboxEvent.getTopic(), application.getId().toString(), event).get();
            outboxEvent.setStatus(OUTBOX_STATUS_PUBLISHED);
            outboxEvent.setPublishedAt(OffsetDateTime.now());
            outboxEventRepository.save(outboxEvent);
            return true;
        } catch (Exception ex) {
            log.warn("Failed to publish outbox event {}", outboxEvent.getId(), ex);
            return false;
        }
    }
}
