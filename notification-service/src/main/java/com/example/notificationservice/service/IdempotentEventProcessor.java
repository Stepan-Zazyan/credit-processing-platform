package com.example.notificationservice.service;

import com.example.notificationservice.entity.ProcessedEvent;
import com.example.notificationservice.repository.ProcessedEventRepository;
import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class IdempotentEventProcessor {
    private final ProcessedEventRepository processedEventRepository;

    public IdempotentEventProcessor(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    public boolean process(String eventId, Runnable businessLogic) {
        if (processedEventRepository.existsById(eventId)) {
            log.info("Event {} already processed, skipping", eventId);
            return false;
        }

        businessLogic.run();
        processedEventRepository.save(new ProcessedEvent(eventId, OffsetDateTime.now()));
        return true;
    }
}
