package com.example.creditapplicationservice.service;

import com.example.creditapplicationservice.dto.ApplicationCreatedEvent;
import com.example.creditapplicationservice.dto.ApplicationRequest;
import com.example.creditapplicationservice.dto.ApplicationResponse;
import com.example.creditapplicationservice.dto.ScoringDecisionResponse;
import com.example.creditapplicationservice.entity.Application;
import com.example.creditapplicationservice.entity.ApplicationStatus;
import com.example.creditapplicationservice.entity.IdempotencyRecord;
import com.example.creditapplicationservice.entity.OutboxEventEntity;
import com.example.creditapplicationservice.entity.OutboxEventStatus;
import com.example.creditapplicationservice.mapper.ApplicationMapper;
import com.example.creditapplicationservice.repository.ApplicationRepository;
import com.example.creditapplicationservice.repository.IdempotencyRecordRepository;
import com.example.creditapplicationservice.repository.OutboxEventRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApplicationService {
    private static final String TOPIC = "credit.application.created";
    private static final int IN_PROGRESS_STATUS = HttpStatus.PROCESSING.value();

    private final ApplicationRepository applicationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ScoringClient scoringClient;
    private final ApplicationMapper applicationMapper;

    @Transactional
    public CreateApplicationResult create(ApplicationRequest request, String idempotencyKey) {
        IdempotencyRecord existing = idempotencyRecordRepository.findByKey(idempotencyKey).orElse(null);
        if (existing != null) {
            String requestHash = request.getClientName() + "|" + request.getAmount();
            if (!requestHash.equals(existing.getRequestHash())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Idempotency-Key уже использован с другим телом запроса");
            }
            if (existing.getStatus() == IN_PROGRESS_STATUS) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Запрос с этим Idempotency-Key уже обрабатывается");
            }

            String[] parts = existing.getResponseBody().split("\\|", -1);
            return new CreateApplicationResult(existing.getStatus(), Map.of(
                    "id", UUID.fromString(parts[0]),
                    "status", parts[1]
            ));
        }

        String requestHash = request.getClientName() + "|" + request.getAmount();
        try {
            idempotencyRecordRepository.saveAndFlush(new IdempotencyRecord(
                    idempotencyKey,
                    requestHash,
                    "",
                    IN_PROGRESS_STATUS
            ));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Запрос с этим Idempotency-Key уже обрабатывается");
        }

        ScoringDecisionResponse scoringDecision = scoringClient.getDecision(request.getClientName());
        log.info("Scoring decision for clientName={} decision={} fallback={}",
                request.getClientName(), scoringDecision.decision(), scoringDecision.fallback());

        Application saved = applicationRepository.save(new Application(
                UUID.randomUUID(),
                request.getClientName(),
                request.getAmount(),
                ApplicationStatus.CREATED
        ));

        ApplicationCreatedEvent event = new ApplicationCreatedEvent(
                UUID.randomUUID(),
                saved.getId(),
                saved.getClientName(),
                saved.getAmount(),
                saved.getStatus().name()
        );
        outboxEventRepository.save(new OutboxEventEntity(
                event.getEventId(),
                saved.getId(),
                TOPIC,
                ApplicationCreatedEvent.class.getSimpleName(),
                String.format("{\"eventId\":\"%s\",\"applicationId\":\"%s\",\"clientName\":\"%s\",\"amount\":%s,\"status\":\"%s\"}",
                        event.getEventId(), event.getApplicationId(), event.getClientName(), event.getAmount(), event.getStatus()),
                OutboxEventStatus.NEW
        ));

        IdempotencyRecord record = idempotencyRecordRepository.findByKey(idempotencyKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Idempotency record not found"));
        record.setStatus(HttpStatus.CREATED.value());
        record.setResponseBody(saved.getId() + "|" + saved.getStatus().name());
        idempotencyRecordRepository.save(record);

        return new CreateApplicationResult(HttpStatus.CREATED.value(), Map.of(
                "id", saved.getId(),
                "status", saved.getStatus().name()
        ));
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getById(UUID id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        return applicationMapper.toResponse(application);
    }

    @Transactional
    public void markApproved(UUID applicationId) {
        updateStatus(applicationId, ApplicationStatus.APPROVED);
    }

    @Transactional
    public void markRejected(UUID applicationId) {
        updateStatus(applicationId, ApplicationStatus.REJECTED);
    }

    @Transactional
    public void updateStatus(UUID applicationId, ApplicationStatus status) {
        if (applicationId == null) {
            log.warn("Decision ignored: empty applicationId");
            return;
        }

        applicationRepository.findById(applicationId).ifPresent(application -> {
            if (application.getStatus() == status) {
                return;
            }
            application.setStatus(status);
            applicationRepository.save(application);
        });
    }
}
