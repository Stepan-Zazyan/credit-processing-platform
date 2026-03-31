package com.example.creditapplicationservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.creditapplicationservice.dto.ApplicationRequest;
import com.example.creditapplicationservice.entity.Application;
import com.example.creditapplicationservice.entity.OutboxEventEntity;
import com.example.creditapplicationservice.entity.OutboxEventStatus;
import com.example.creditapplicationservice.repository.ApplicationRepository;
import com.example.creditapplicationservice.repository.IdempotencyRecordRepository;
import com.example.creditapplicationservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createStoresOutboxRecordAndIdempotencyResponse() throws Exception {
        ApplicationService applicationService = new ApplicationService(
                applicationRepository,
                outboxEventRepository,
                idempotencyRecordRepository,
                jdbcTemplate,
                objectMapper
        );

        ApplicationRequest request = new ApplicationRequest("Ivan Ivanov", new BigDecimal("10000.00"));

        when(jdbcTemplate.update(any(String.class), any(), any())).thenReturn(1);
        when(jdbcTemplate.update(any(String.class), any(), any(), any(), any())).thenReturn(1);
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateApplicationResult result = applicationService.create(request, "idem-1");

        assertThat(result.getStatus()).isEqualTo(201);
        assertThat(result.getBody()).containsKey("id").containsEntry("status", ApplicationService.CREATED_STATUS);

        ArgumentCaptor<OutboxEventEntity> outboxCaptor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());

        OutboxEventEntity outboxEvent = outboxCaptor.getValue();
        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxEventStatus.NEW);

        JsonNode payload = objectMapper.readTree(outboxEvent.getPayload());
        assertThat(UUID.fromString(payload.get("applicationId").asText())).isEqualTo(outboxEvent.getAggregateId());
    }

    @Test
    void approvedEventUpdatesStatus() {
        UUID applicationId = UUID.randomUUID();
        Application application = new Application(applicationId, "John", new BigDecimal("1000.00"), "CREATED");
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        ApplicationService service = new ApplicationService(
                applicationRepository,
                outboxEventRepository,
                idempotencyRecordRepository,
                jdbcTemplate,
                objectMapper
        );
        service.markApproved(applicationId);

        verify(applicationRepository).save(application);
    }

    @Test
    void duplicateEventDoesNotBreakProcessing() {
        UUID applicationId = UUID.randomUUID();
        Application application = new Application(applicationId, "John", new BigDecimal("1000.00"), "APPROVED");
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        ApplicationService service = new ApplicationService(
                applicationRepository,
                outboxEventRepository,
                idempotencyRecordRepository,
                jdbcTemplate,
                objectMapper
        );
        service.markApproved(applicationId);

        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void getByIdReturnsResponse() {
        UUID applicationId = UUID.randomUUID();
        Application application = new Application(applicationId, "John", new BigDecimal("1000.00"), "CREATED");
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        ApplicationService service = new ApplicationService(
                applicationRepository,
                outboxEventRepository,
                idempotencyRecordRepository,
                jdbcTemplate,
                objectMapper
        );

        Map<String, Object> body = Map.of(
                "id", service.getById(applicationId).getId(),
                "status", service.getById(applicationId).getStatus()
        );
        assertThat(body).containsEntry("id", applicationId).containsEntry("status", "CREATED");
    }
}
