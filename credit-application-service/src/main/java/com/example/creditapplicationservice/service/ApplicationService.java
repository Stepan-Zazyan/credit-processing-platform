package com.example.creditapplicationservice.service;

import com.example.creditapplicationservice.dto.ApplicationCreatedEvent;
import com.example.creditapplicationservice.dto.ApplicationRequest;
import com.example.creditapplicationservice.entity.Application;
import com.example.creditapplicationservice.repository.ApplicationRepository;
import java.util.UUID;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationService {
    private static final String CREATED_STATUS = "CREATED";
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
}
