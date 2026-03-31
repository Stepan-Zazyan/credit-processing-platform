package com.example.creditapplicationservice.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.creditapplicationservice.dto.ApplicationCreatedEvent;
import com.example.creditapplicationservice.entity.Application;
import com.example.creditapplicationservice.repository.ApplicationRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private KafkaTemplate<String, ApplicationCreatedEvent> kafkaTemplate;

    @Test
    void approvedEventUpdatesStatus() {
        UUID applicationId = UUID.randomUUID();
        Application application = new Application(applicationId, "John", new BigDecimal("1000.00"), "CREATED");
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        ApplicationService service = new ApplicationService(applicationRepository, kafkaTemplate);
        service.markApproved(applicationId);

        verify(applicationRepository).save(application);
    }

    @Test
    void rejectedEventUpdatesStatus() {
        UUID applicationId = UUID.randomUUID();
        Application application = new Application(applicationId, "John", new BigDecimal("1000.00"), "CREATED");
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        ApplicationService service = new ApplicationService(applicationRepository, kafkaTemplate);
        service.markRejected(applicationId);

        verify(applicationRepository).save(application);
    }

    @Test
    void duplicateEventDoesNotBreakProcessing() {
        UUID applicationId = UUID.randomUUID();
        Application application = new Application(applicationId, "John", new BigDecimal("1000.00"), "APPROVED");
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        ApplicationService service = new ApplicationService(applicationRepository, kafkaTemplate);
        service.markApproved(applicationId);

        verify(applicationRepository, never()).save(any(Application.class));
    }
}
