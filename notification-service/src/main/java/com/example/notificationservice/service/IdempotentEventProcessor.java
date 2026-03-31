package com.example.notificationservice.service;

import com.example.notificationservice.entity.ProcessedEvent;
import com.example.notificationservice.repository.ProcessedEventRepository;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotentEventProcessor {
    private static final Logger logger = LoggerFactory.getLogger(IdempotentEventProcessor.class);

    private final ProcessedEventRepository processedEventRepository;

    public IdempotentEventProcessor(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    public boolean process(String eventId, Runnable businessLogic) {
        if (processedEventRepository.existsById(eventId)) {
            logger.info("Event {} already processed, skipping", eventId);
            return false;
        }

        businessLogic.run();
        processedEventRepository.save(new ProcessedEvent(eventId, OffsetDateTime.now()));
        return true;
    }
}
