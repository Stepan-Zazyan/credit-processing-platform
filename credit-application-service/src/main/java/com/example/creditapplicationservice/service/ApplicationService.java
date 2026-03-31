package com.example.creditapplicationservice.service;

import com.example.creditapplicationservice.dto.ApplicationRequest;
import com.example.creditapplicationservice.entity.Application;
import com.example.creditapplicationservice.entity.OutboxEvent;
import com.example.creditapplicationservice.repository.ApplicationRepository;
import com.example.creditapplicationservice.repository.OutboxEventRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationService {
    public static final String CREATED_STATUS = "CREATED";
    private static final String TOPIC = "credit.application.created";

    private final ApplicationRepository applicationRepository;
    private final OutboxEventRepository outboxEventRepository;

    public ApplicationService(ApplicationRepository applicationRepository,
                              OutboxEventRepository outboxEventRepository) {
        this.applicationRepository = applicationRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public Application create(ApplicationRequest request) {
        UUID id = UUID.randomUUID();
        Application application = new Application(id, request.getClientName(), request.getAmount(), CREATED_STATUS);
        Application saved = applicationRepository.save(application);

        OutboxEvent outboxEvent = new OutboxEvent(
                UUID.randomUUID(),
                saved.getId(),
                TOPIC,
                "ApplicationCreatedEvent",
                OutboxPublisherService.OUTBOX_STATUS_NEW
        );
        outboxEventRepository.save(outboxEvent);

        return saved;
    }
}
