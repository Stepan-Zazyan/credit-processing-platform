package com.example.creditapplicationservice.repository;

import com.example.creditapplicationservice.entity.Application;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    List<Application> findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(String status, OffsetDateTime threshold);
}
