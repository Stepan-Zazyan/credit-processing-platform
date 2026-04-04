package com.example.creditapplicationservice.controller;

import com.example.creditapplicationservice.dto.ApplicationRequest;
import com.example.creditapplicationservice.dto.ApplicationResponse;
import com.example.creditapplicationservice.service.ApplicationService;
import com.example.creditapplicationservice.service.CreateApplicationResult;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {
    private final ApplicationService applicationService;

    @Operation(summary = "Create credit application")
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ApplicationRequest request
    ) {
        CreateApplicationResult result = applicationService.create(request, idempotencyKey);
        return ResponseEntity.status(result.getStatus()).body(result.getBody());
    }

    @Operation(summary = "Get application by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(applicationService.getById(id));
    }
}
