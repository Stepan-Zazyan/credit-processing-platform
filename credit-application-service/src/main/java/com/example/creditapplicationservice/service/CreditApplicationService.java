package com.example.creditapplicationservice.service;

import com.example.creditapplicationservice.event.ApplicationCreatedEvent;
import com.example.creditapplicationservice.model.ApplicationStatus;
import com.example.creditapplicationservice.model.CreditApplication;
import com.example.creditapplicationservice.repository.CreditApplicationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for creating and storing credit applications.
 */
@Service
public class CreditApplicationService {

    public static final String APPLICATION_CREATED_TOPIC = "credit.application.created";

    private final CreditApplicationRepository repository;
    private final KafkaTemplate<String, ApplicationCreatedEvent> kafkaTemplate;

    public CreditApplicationService(CreditApplicationRepository repository,
                                    KafkaTemplate<String, ApplicationCreatedEvent> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public CreditApplication createApplication(String clientName, BigDecimal amount) {
        CreditApplication application = new CreditApplication(
                UUID.randomUUID(),
                clientName,
                amount,
                ApplicationStatus.NEW,
                Instant.now()
        );
        repository.save(application);

        ApplicationCreatedEvent event = new ApplicationCreatedEvent(
                application.getId(),
                application.getClientName(),
                application.getAmount(),
                application.getStatus().name(),
                application.getCreatedAt()
        );
        kafkaTemplate.send(APPLICATION_CREATED_TOPIC, application.getId().toString(), event);
        return application;
    }
}
