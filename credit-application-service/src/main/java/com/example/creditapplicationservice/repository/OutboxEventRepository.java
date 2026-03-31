package com.example.creditapplicationservice.repository;

import com.example.creditapplicationservice.entity.OutboxEvent;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(String status);

    List<OutboxEvent> findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(String status, OffsetDateTime threshold);
}
