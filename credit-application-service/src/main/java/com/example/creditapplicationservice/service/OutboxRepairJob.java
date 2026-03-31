package com.example.creditapplicationservice.service;

import com.example.creditapplicationservice.config.RepairJobProperties;
import com.example.creditapplicationservice.entity.Application;
import com.example.creditapplicationservice.entity.OutboxEventEntity;
import com.example.creditapplicationservice.entity.OutboxEventStatus;
import com.example.creditapplicationservice.repository.ApplicationRepository;
import com.example.creditapplicationservice.repository.OutboxEventRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxRepairJob {
    private static final Logger log = LoggerFactory.getLogger(OutboxRepairJob.class);

    private final RepairJobProperties repairJobProperties;
    private final OutboxEventRepository outboxEventRepository;
    private final ApplicationRepository applicationRepository;
    private final OutboxPublisher outboxPublisher;

    public OutboxRepairJob(RepairJobProperties repairJobProperties,
                           OutboxEventRepository outboxEventRepository,
                           ApplicationRepository applicationRepository,
                           OutboxPublisher outboxPublisher) {
        this.repairJobProperties = repairJobProperties;
        this.outboxEventRepository = outboxEventRepository;
        this.applicationRepository = applicationRepository;
        this.outboxPublisher = outboxPublisher;
    }

    @Scheduled(fixedDelayString = "${credit.application.repair.polling-interval-ms:30000}")
    public void reconcile() {
        OffsetDateTime thresholdTime = OffsetDateTime.now().minus(repairJobProperties.getThreshold());

        List<OutboxEventEntity> staleEvents = outboxEventRepository.findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                OutboxEventStatus.NEW,
                thresholdTime
        );

        for (OutboxEventEntity staleEvent : staleEvents) {
            log.warn("Found stale NEW outbox event id={} aggregateId={} createdAt={}",
                    staleEvent.getEventId(), staleEvent.getAggregateId(), staleEvent.getCreatedAt());
            outboxPublisher.publishEvent(staleEvent);
        }

        if (repairJobProperties.isCheckStuckApplications()) {
            List<Application> potentiallyStuck = applicationRepository.findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                    ApplicationService.CREATED_STATUS,
                    thresholdTime
            );
            for (Application application : potentiallyStuck) {
                log.warn("Potentially stuck application id={} status={} createdAt={}",
                        application.getId(), application.getStatus(), application.getCreatedAt());
            }
        }
    }
}
