package com.example.creditapplicationservice.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.creditapplicationservice.config.RepairJobProperties;
import com.example.creditapplicationservice.entity.Application;
import com.example.creditapplicationservice.entity.OutboxEvent;
import com.example.creditapplicationservice.repository.ApplicationRepository;
import com.example.creditapplicationservice.repository.OutboxEventRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxRepairJobTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private OutboxPublisherService outboxPublisherService;

    @Test
    void shouldRepublishStaleNewEventsAndCheckStuckApplications() {
        RepairJobProperties properties = new RepairJobProperties();
        properties.setThreshold(Duration.ofMinutes(10));
        properties.setCheckStuckApplications(true);

        OutboxRepairJob job = new OutboxRepairJob(properties, outboxEventRepository, applicationRepository, outboxPublisherService);

        OutboxEvent staleEvent = new OutboxEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "credit.application.created",
                "ApplicationCreatedEvent",
                OutboxPublisherService.OUTBOX_STATUS_NEW
        );

        Application staleApplication = new Application(UUID.randomUUID(), "John", BigDecimal.TEN, ApplicationService.CREATED_STATUS);

        when(outboxEventRepository.findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(eq(OutboxPublisherService.OUTBOX_STATUS_NEW), any(OffsetDateTime.class)))
                .thenReturn(List.of(staleEvent));
        when(applicationRepository.findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(eq(ApplicationService.CREATED_STATUS), any(OffsetDateTime.class)))
                .thenReturn(List.of(staleApplication));

        job.reconcile();

        verify(outboxPublisherService).publishEvent(staleEvent);
        verify(applicationRepository).findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(eq(ApplicationService.CREATED_STATUS), any(OffsetDateTime.class));
    }

    @Test
    void shouldSkipStuckApplicationsCheckWhenDisabled() {
        RepairJobProperties properties = new RepairJobProperties();
        properties.setCheckStuckApplications(false);

        OutboxRepairJob job = new OutboxRepairJob(properties, outboxEventRepository, applicationRepository, outboxPublisherService);

        when(outboxEventRepository.findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(eq(OutboxPublisherService.OUTBOX_STATUS_NEW), any(OffsetDateTime.class)))
                .thenReturn(List.of());

        job.reconcile();

        verify(applicationRepository, never()).findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(any(), any());
    }
}
