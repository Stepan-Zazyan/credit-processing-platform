package com.example.creditapplicationservice.listener;

import com.example.creditapplicationservice.dto.ApplicationDecisionEvent;
import com.example.creditapplicationservice.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ApplicationDecisionListener {
    private final ApplicationService applicationService;

    @KafkaListener(topics = "credit.application.approved", groupId = "credit-application-service")
    public void onApproved(ApplicationDecisionEvent event, Acknowledgment acknowledgment) {
        applicationService.markApproved(event.getApplicationId());
        log.info("Processed approved decision eventId={} applicationId={}", event.getEventId(), event.getApplicationId());
        acknowledgment.acknowledge();
    }

    @KafkaListener(topics = "credit.application.rejected", groupId = "credit-application-service")
    public void onRejected(ApplicationDecisionEvent event, Acknowledgment acknowledgment) {
        applicationService.markRejected(event.getApplicationId());
        log.info("Processed rejected decision eventId={} applicationId={} reason={}",
                event.getEventId(), event.getApplicationId(), event.getReason());
        acknowledgment.acknowledge();
    }
}
