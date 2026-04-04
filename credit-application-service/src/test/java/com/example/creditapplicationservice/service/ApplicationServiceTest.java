package com.example.creditapplicationservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.creditapplicationservice.dto.ApplicationRequest;
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
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;
    @Mock
    private ScoringClient scoringClient;
    @Mock
    private ApplicationMapper applicationMapper;

    @Test
    void createStoresOutboxRecordAndIdempotencyResponse() {
        ApplicationService applicationService = new ApplicationService(
                applicationRepository,
                outboxEventRepository,
                idempotencyRecordRepository,
                scoringClient,
                applicationMapper
        );

        ApplicationRequest request = new ApplicationRequest("Ivan Ivanov", new BigDecimal("10000.00"));

        when(idempotencyRecordRepository.findByKey("idem-1")).thenReturn(Optional.empty(), Optional.of(
                new IdempotencyRecord("idem-1", "Ivan Ivanov|10000.00", "", 102)
        ));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(scoringClient.getDecision(any())).thenReturn(new ScoringDecisionResponse("APPROVE", false));

        CreateApplicationResult result = applicationService.create(request, "idem-1");

        assertThat(result.getStatus()).isEqualTo(201);
        assertThat(result.getBody()).containsKeys("id", "status");

        ArgumentCaptor<OutboxEventEntity> outboxCaptor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());

        OutboxEventEntity outboxEvent = outboxCaptor.getValue();
        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxEventStatus.NEW);
        assertThat(outboxEvent.getPayload()).contains("applicationId");
    }

    @Test
    void approvedEventUpdatesStatus() {
        UUID applicationId = UUID.randomUUID();
        Application application = new Application(applicationId, "John", new BigDecimal("1000.00"), ApplicationStatus.CREATED);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        ApplicationService service = new ApplicationService(
                applicationRepository,
                outboxEventRepository,
                idempotencyRecordRepository,
                scoringClient,
                applicationMapper
        );
        service.markApproved(applicationId);

        verify(applicationRepository).save(application);
    }

    @Test
    void duplicateEventDoesNotBreakProcessing() {
        UUID applicationId = UUID.randomUUID();
        Application application = new Application(applicationId, "John", new BigDecimal("1000.00"), ApplicationStatus.APPROVED);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        ApplicationService service = new ApplicationService(
                applicationRepository,
                outboxEventRepository,
                idempotencyRecordRepository,
                scoringClient,
                applicationMapper
        );
        service.markApproved(applicationId);

        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void createReturnsStoredResponseForUsedIdempotencyKey() {
        UUID id = UUID.randomUUID();
        when(idempotencyRecordRepository.findByKey("idem-1"))
                .thenReturn(Optional.of(new IdempotencyRecord("idem-1", "John|1000", id + "|CREATED", 201)));

        ApplicationService service = new ApplicationService(
                applicationRepository,
                outboxEventRepository,
                idempotencyRecordRepository,
                scoringClient,
                applicationMapper
        );

        CreateApplicationResult result = service.create(new ApplicationRequest("John", new BigDecimal("1000")), "idem-1");
        assertThat(result.getStatus()).isEqualTo(201);
        assertThat(result.getBody()).containsEntry("id", id).containsEntry("status", "CREATED");
    }

    @Test
    void getByIdReturnsResponse() {
        UUID applicationId = UUID.randomUUID();
        Application application = new Application(applicationId, "John", new BigDecimal("1000.00"), ApplicationStatus.CREATED);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(applicationMapper.toResponse(application))
                .thenReturn(new com.example.creditapplicationservice.dto.ApplicationResponse(applicationId, "John", new BigDecimal("1000.00"), "CREATED"));

        ApplicationService service = new ApplicationService(
                applicationRepository,
                outboxEventRepository,
                idempotencyRecordRepository,
                scoringClient,
                applicationMapper
        );

        Map<String, Object> body = Map.of(
                "id", service.getById(applicationId).getId(),
                "status", service.getById(applicationId).getStatus()
        );
        assertThat(body).containsEntry("id", applicationId).containsEntry("status", "CREATED");
    }
}
