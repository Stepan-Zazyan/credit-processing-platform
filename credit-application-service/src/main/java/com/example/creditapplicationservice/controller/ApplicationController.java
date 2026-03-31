package com.example.creditapplicationservice.controller;

import com.example.creditapplicationservice.dto.ApplicationRequest;
import com.example.creditapplicationservice.service.ApplicationService;
import com.example.creditapplicationservice.service.CreateApplicationResult;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    public ResponseEntity<Map<String, Object>> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody ApplicationRequest request
    ) {
        CreateApplicationResult result = applicationService.create(request, idempotencyKey);
        return ResponseEntity.status(result.getStatus()).body(result.getBody());
    }
}
