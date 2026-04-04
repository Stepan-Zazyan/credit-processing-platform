package com.example.creditapplicationservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.creditapplicationservice.entity.OutboxEventEntity;
import com.example.creditapplicationservice.entity.OutboxEventStatus;
import com.example.creditapplicationservice.repository.OutboxEventRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void publishMarksEventAsPublished() {
        OutboxPublisher outboxPublisher = new OutboxPublisher(outboxEventRepository, kafkaTemplate);

        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();

        OutboxEventEntity outboxEvent = new OutboxEventEntity(
                eventId,
                aggregateId,
                "credit.application.created",
                "ApplicationCreatedEvent",
                "{\"applicationId\":\"" + aggregateId + "\"}",
                OutboxEventStatus.NEW
        );

        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW))
                .thenReturn(List.of(outboxEvent));
        when(kafkaTemplate.send(any(String.class), any(String.class), any(String.class)))
                .thenReturn(CompletableFuture.completedFuture((SendResult<String, String>) null));

        outboxPublisher.publishNewEvents();

        ArgumentCaptor<OutboxEventEntity> savedCaptor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).save(savedCaptor.capture());
        OutboxEventEntity saved = savedCaptor.getValue();

        assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(saved.getPublishedAt()).isNotNull();
    }
}
