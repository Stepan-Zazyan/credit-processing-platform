package com.example.creditapplicationservice.service;

import com.example.creditapplicationservice.dto.ApplicationCreatedEvent;
import com.example.creditapplicationservice.dto.ApplicationRequest;
import com.example.creditapplicationservice.entity.Application;
import com.example.creditapplicationservice.repository.ApplicationRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationService {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationService.class);
    private static final String CREATED_STATUS = "CREATED";
    private static final String APPROVED_STATUS = "APPROVED";
    private static final String REJECTED_STATUS = "REJECTED";
    private static final String TOPIC = "credit.application.created";

    private final ApplicationRepository applicationRepository;
    private final KafkaTemplate<String, ApplicationCreatedEvent> kafkaTemplate;

    public ApplicationService(ApplicationRepository applicationRepository,
                              KafkaTemplate<String, ApplicationCreatedEvent> kafkaTemplate) {
        this.applicationRepository = applicationRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public Application create(ApplicationRequest request) {
        UUID id = UUID.randomUUID();
        Application application = new Application(id, request.getClientName(), request.getAmount(), CREATED_STATUS);
        Application saved = applicationRepository.save(application);
        ApplicationCreatedEvent event = new ApplicationCreatedEvent(saved.getId(), saved.getClientName(),
                saved.getAmount(), saved.getStatus());
        kafkaTemplate.send(TOPIC, saved.getId().toString(), event);
        return saved;
    }

    @Transactional
    public void markApproved(UUID applicationId) {
        updateStatus(applicationId, APPROVED_STATUS);
    }

    @Transactional
    public void markRejected(UUID applicationId) {
        updateStatus(applicationId, REJECTED_STATUS);
    }

    private void updateStatus(UUID applicationId, String targetStatus) {
        if (applicationId == null) {
            logger.warn("Skipping decision processing: applicationId is null");
            return;
        }

        applicationRepository.findById(applicationId)
                .ifPresentOrElse(application -> {
                    if (targetStatus.equals(application.getStatus())) {
                        logger.info("Duplicate decision received for application {} with status {}, skipping update",
                                applicationId, targetStatus);
                        return;
                    }
                    application.setStatus(targetStatus);
                    applicationRepository.save(application);
                    logger.info("Application {} status updated to {}", applicationId, targetStatus);
                }, () -> logger.warn("Application {} not found, decision event ignored", applicationId));
    }
}
