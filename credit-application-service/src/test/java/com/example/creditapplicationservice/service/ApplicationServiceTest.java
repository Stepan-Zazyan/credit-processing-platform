package com.example.creditapplicationservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.creditapplicationservice.dto.ApplicationRequest;
import com.example.creditapplicationservice.entity.Application;
import com.example.creditapplicationservice.entity.OutboxEventEntity;
import com.example.creditapplicationservice.entity.OutboxEventStatus;
import com.example.creditapplicationservice.repository.ApplicationRepository;
import com.example.creditapplicationservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createStoresOutboxRecord() throws Exception {
        ApplicationService applicationService = new ApplicationService(applicationRepository, outboxEventRepository, objectMapper);

        ApplicationRequest request = new ApplicationRequest();
        request.setClientName("Ivan Ivanov");
        request.setAmount(new BigDecimal("10000.00"));

        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Application result = applicationService.create(request);

        verify(applicationRepository).save(any(Application.class));

        ArgumentCaptor<OutboxEventEntity> outboxCaptor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());

        OutboxEventEntity outboxEvent = outboxCaptor.getValue();
        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxEventStatus.NEW);
        assertThat(outboxEvent.getAggregateId()).isEqualTo(result.getId());

        JsonNode payload = objectMapper.readTree(outboxEvent.getPayload());
        assertThat(payload.get("eventId").asText()).isEqualTo(outboxEvent.getEventId().toString());
        assertThat(UUID.fromString(payload.get("applicationId").asText())).isEqualTo(result.getId());
    }
}
