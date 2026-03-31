package com.example.creditapplicationservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.creditapplicationservice.dto.ApplicationRequest;
import com.example.creditapplicationservice.entity.Application;
import com.example.creditapplicationservice.entity.OutboxEvent;
import com.example.creditapplicationservice.repository.ApplicationRepository;
import com.example.creditapplicationservice.repository.OutboxEventRepository;
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

    @Test
    void shouldCreateApplicationAndOutboxEvent() {
        ApplicationService applicationService = new ApplicationService(applicationRepository, outboxEventRepository);

        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationRequest request = new ApplicationRequest();
        request.setClientName("Alice");
        request.setAmount(new BigDecimal("1000.00"));

        Application saved = applicationService.create(request);

        assertThat(saved.getStatus()).isEqualTo(ApplicationService.CREATED_STATUS);
        verify(applicationRepository).save(any(Application.class));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        OutboxEvent outboxEvent = captor.getValue();

        assertThat(outboxEvent.getAggregateId()).isEqualTo(saved.getId());
        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxPublisherService.OUTBOX_STATUS_NEW);
        assertThat(outboxEvent.getTopic()).isEqualTo("credit.application.created");
        assertThat(outboxEvent.getEventType()).isEqualTo("ApplicationCreatedEvent");
    }
}
