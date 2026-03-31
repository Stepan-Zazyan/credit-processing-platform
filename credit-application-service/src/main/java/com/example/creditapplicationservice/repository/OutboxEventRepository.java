package com.example.creditapplicationservice.repository;

import com.example.creditapplicationservice.entity.OutboxEventEntity;
import com.example.creditapplicationservice.entity.OutboxEventStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
    List<OutboxEventEntity> findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus status);

    List<OutboxEventEntity> findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
            OutboxEventStatus status,
            OffsetDateTime threshold
    );
}
