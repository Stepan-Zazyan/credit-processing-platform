package com.example.creditapplicationservice.controller;

import com.example.creditapplicationservice.dto.ApplicationRequest;
import com.example.creditapplicationservice.entity.Application;
import com.example.creditapplicationservice.service.ApplicationService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/applications")
public class ApplicationController {
    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody ApplicationRequest request) {
        Application application = applicationService.create(request);
        Map<String, Object> body = Map.of(
                "id", application.getId(),
                "status", application.getStatus()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
