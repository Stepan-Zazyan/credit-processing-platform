package com.example.creditapplicationservice.repository;

import com.example.creditapplicationservice.entity.Application;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {
}
