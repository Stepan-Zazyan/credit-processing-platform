package com.example.creditapplicationservice.service;

import com.example.creditapplicationservice.dto.ApplicationCreatedEvent;
import com.example.creditapplicationservice.dto.ApplicationRequest;
import com.example.creditapplicationservice.entity.Application;
import com.example.creditapplicationservice.entity.OutboxEventEntity;
import com.example.creditapplicationservice.entity.OutboxEventStatus;
import com.example.creditapplicationservice.repository.ApplicationRepository;
import com.example.creditapplicationservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationService {
    private static final String CREATED_STATUS = "CREATED";
    private static final String TOPIC = "credit.application.created";

    private final ApplicationRepository applicationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public ApplicationService(ApplicationRepository applicationRepository,
                              OutboxEventRepository outboxEventRepository,
                              ObjectMapper objectMapper) {
        this.applicationRepository = applicationRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Application create(ApplicationRequest request) {
        UUID applicationId = UUID.randomUUID();
        Application application = new Application(applicationId, request.getClientName(), request.getAmount(), CREATED_STATUS);
        Application saved = applicationRepository.save(application);

        UUID eventId = UUID.randomUUID();
        ApplicationCreatedEvent eventPayload = new ApplicationCreatedEvent(
                eventId,
                saved.getId(),
                saved.getClientName(),
                saved.getAmount(),
                saved.getStatus()
        );

        OutboxEventEntity outboxEvent = new OutboxEventEntity(
                eventId,
                saved.getId(),
                TOPIC,
                ApplicationCreatedEvent.class.getSimpleName(),
                toJson(eventPayload),
                OutboxEventStatus.NEW
        );
        outboxEventRepository.save(outboxEvent);

        return saved;
    }

    private String toJson(ApplicationCreatedEvent eventPayload) {
        try {
            return objectMapper.writeValueAsString(eventPayload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize application created event", e);
        }
    }
}
