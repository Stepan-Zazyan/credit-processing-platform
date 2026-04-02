package com.example.creditapplicationservice.service;

import com.example.creditapplicationservice.dto.ApplicationCreatedEvent;
import com.example.creditapplicationservice.dto.ApplicationRequest;
import com.example.creditapplicationservice.dto.ApplicationResponse;
import com.example.creditapplicationservice.dto.ScoringDecisionResponse;
import com.example.creditapplicationservice.entity.Application;
import com.example.creditapplicationservice.entity.IdempotencyRecord;
import com.example.creditapplicationservice.entity.OutboxEventEntity;
import com.example.creditapplicationservice.entity.OutboxEventStatus;
import com.example.creditapplicationservice.repository.ApplicationRepository;
import com.example.creditapplicationservice.repository.IdempotencyRecordRepository;
import com.example.creditapplicationservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApplicationService {
    public static final String CREATED_STATUS = "CREATED";
    private static final String APPROVED_STATUS = "APPROVED";
    private static final String REJECTED_STATUS = "REJECTED";
    private static final String TOPIC = "credit.application.created";

    private static final Logger logger = LoggerFactory.getLogger(ApplicationService.class);

    private final ApplicationRepository applicationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final ScoringClient scoringClient;

    public ApplicationService(ApplicationRepository applicationRepository,
                              OutboxEventRepository outboxEventRepository,
                              IdempotencyRecordRepository idempotencyRecordRepository,
                              JdbcTemplate jdbcTemplate,
                              ObjectMapper objectMapper,
                              ScoringClient scoringClient) {
        this.applicationRepository = applicationRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.scoringClient = scoringClient;
    }

    @Transactional
    public CreateApplicationResult create(ApplicationRequest request, String idempotencyKey) {
        ScoringDecisionResponse scoringDecision = safeScoringDecision(request.getClientName());
        logger.info("Scoring decision for clientName={} decision={} fallback={}",
                request.getClientName(), scoringDecision.decision(), scoringDecision.fallback());

        String requestHash = hashRequest(request);
        int inserted = jdbcTemplate.update(
                """
                INSERT INTO idempotency_records (key, request_hash, response_body, status)
                VALUES (?, ?, '{}', 0)
                ON CONFLICT DO NOTHING
                """,
                idempotencyKey, requestHash
        );

        if (inserted == 0) {
            IdempotencyRecord existing = idempotencyRecordRepository.findById(idempotencyKey)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Idempotency record not found"));

            if (!existing.getRequestHash().equals(requestHash)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Idempotency-Key уже использован с другим телом запроса");
            }

            if (existing.getStatus() == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Запрос с этим Idempotency-Key уже обрабатывается");
            }

            return new CreateApplicationResult(existing.getStatus(), parseBody(existing.getResponseBody()));
        }

        Application created = saveApplicationAndStoreOutboxEvent(request);
        Map<String, Object> body = Map.of("id", created.getId(), "status", created.getStatus());

        int updated = jdbcTemplate.update(
                """
                UPDATE idempotency_records
                SET response_body = ?, status = ?
                WHERE key = ? AND request_hash = ?
                """,
                toJson(body),
                HttpStatus.CREATED.value(),
                idempotencyKey,
                requestHash
        );

        if (updated != 1) {
            throw new DataIntegrityViolationException("Failed to update idempotency record");
        }

        return new CreateApplicationResult(HttpStatus.CREATED.value(), body);
    }

    private ScoringDecisionResponse safeScoringDecision(String clientName) {
        try {
            return scoringClient.getDecision(clientName).join();
        } catch (CompletionException ex) {
            logger.warn("Scoring call failed for clientName={}, using local fallback", clientName, ex);
            return ScoringDecisionResponse.fallback();
        }
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getById(UUID id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        return new ApplicationResponse(
                application.getId(),
                application.getClientName(),
                application.getAmount(),
                application.getStatus()
        );
    }

    @Transactional
    public void markApproved(UUID applicationId) {
        updateStatus(applicationId, APPROVED_STATUS);
    }

    @Transactional
    public void markRejected(UUID applicationId) {
        updateStatus(applicationId, REJECTED_STATUS);
    }

    private Application saveApplicationAndStoreOutboxEvent(ApplicationRequest request) {
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

    private String hashRequest(ApplicationRequest request) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String payload = request.getClientName() + "|" + request.getAmount();
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
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
