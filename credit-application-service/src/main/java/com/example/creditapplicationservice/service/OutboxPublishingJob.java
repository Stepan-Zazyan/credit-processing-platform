package com.example.creditapplicationservice.service;

import com.example.creditapplicationservice.entity.OutboxEvent;
import com.example.creditapplicationservice.repository.OutboxEventRepository;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublishingJob {
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxPublisherService outboxPublisherService;

    public OutboxPublishingJob(OutboxEventRepository outboxEventRepository,
                               OutboxPublisherService outboxPublisherService) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxPublisherService = outboxPublisherService;
    }

    @Scheduled(fixedDelayString = "${credit.application.outbox.publisher.polling-interval-ms:5000}")
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(
                OutboxPublisherService.OUTBOX_STATUS_NEW
        );
        pending.forEach(outboxPublisherService::publishEvent);
    }
}
