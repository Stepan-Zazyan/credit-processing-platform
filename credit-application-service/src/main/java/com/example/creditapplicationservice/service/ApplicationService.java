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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
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
    private static final int IN_PROGRESS_STATUS = 0;
    private static final String TOPIC = "credit.application.created";

    private final ApplicationRepository applicationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;
    private final ScoringClient scoringClient;
    private final ApplicationMapper applicationMapper;

    @Transactional
    public CreateApplicationResult create(ApplicationRequest request, String idempotencyKey) {
        ScoringDecisionResponse scoringDecision = safeScoringDecision(request.getClientName());
        log.info("Scoring decision for clientName={} decision={} fallback={}",
                request.getClientName(), scoringDecision.getDecision(), ScoringDecisionResponse.fallback());

        String requestHash = buildRequestFingerprint(request);
        if (!startIdempotentRequest(idempotencyKey, requestHash)) {
            IdempotencyRecord existing = idempotencyRecordRepository.findById(idempotencyKey)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Idempotency record not found"));

            if (!existing.getRequestHash().equals(requestHash)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Idempotency-Key уже использован с другим телом запроса");
            }

            if (existing.getStatus() == IN_PROGRESS_STATUS) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Запрос с этим Idempotency-Key уже обрабатывается");
            }

            return new CreateApplicationResult(existing.getStatus(), parseBody(existing.getResponseBody()));
        }

        Application created = saveApplicationAndStoreOutboxEvent(request);
        Map<String, Object> body = Map.of("id", created.getId(), "status", created.getStatus().name());

        IdempotencyRecord record = idempotencyRecordRepository.findById(idempotencyKey)
                .orElseThrow(() -> new DataIntegrityViolationException("Failed to find idempotency record"));
        record.setResponseBody(toJson(body));
        record.setStatus(HttpStatus.CREATED.value());
        idempotencyRecordRepository.save(record);

        return new CreateApplicationResult(HttpStatus.CREATED.value(), body);
    }

    private ScoringDecisionResponse safeScoringDecision(String clientName) {
        try {
            return scoringClient.getDecision(clientName).join();
        } catch (CompletionException ex) {
            log.warn("Scoring call failed for clientName={}, using local fallback", clientName, ex);
            return ScoringDecisionResponse.fallback();
        }
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

    private Application saveApplicationAndStoreOutboxEvent(ApplicationRequest request) {
        UUID applicationId = UUID.randomUUID();
        Application application = new Application(applicationId, request.getClientName(), request.getAmount(), ApplicationStatus.CREATED);
        Application saved = applicationRepository.save(application);

        UUID eventId = UUID.randomUUID();
        ApplicationCreatedEvent eventPayload = new ApplicationCreatedEvent(
                eventId,
                saved.getId(),
                saved.getClientName(),
                saved.getAmount(),
                saved.getStatus().name()
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

    private void updateStatus(UUID applicationId, ApplicationStatus targetStatus) {
        if (applicationId == null) {
            log.warn("Skipping decision processing: applicationId is null");
            return;
        }

        applicationRepository.findById(applicationId)
                .ifPresentOrElse(application -> {
                    if (targetStatus.equals(application.getStatus())) {
                        log.info("Duplicate decision received for application {} with status {}, skipping update",
                                applicationId, targetStatus);
                        return;
                    }
                    application.setStatus(targetStatus);
                    applicationRepository.save(application);
                    log.info("Application {} status updated to {}", applicationId, targetStatus);
                }, () -> log.warn("Application {} not found, decision event ignored", applicationId));
    }

    private boolean startIdempotentRequest(String idempotencyKey, String requestHash) {
        try {
            idempotencyRecordRepository.saveAndFlush(
                    new IdempotencyRecord(idempotencyKey, requestHash, "{}", IN_PROGRESS_STATUS)
            );
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }

    private String buildRequestFingerprint(ApplicationRequest request) {
        String payload = request.getClientName() + "|" + request.getAmount();
        return UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private Map<String, Object> parseBody(String responseBody) {
        try {
            return objectMapper.readValue(responseBody, new TypeReference<>() { });
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Stored idempotent response is invalid", e);
        }
    }

    private String toJson(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to serialize payload", e);
        }
    }
}
