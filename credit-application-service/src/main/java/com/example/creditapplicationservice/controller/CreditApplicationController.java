package com.example.creditapplicationservice.controller;

import com.example.creditapplicationservice.model.CreditApplication;
import com.example.creditapplicationservice.service.CreditApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing credit application submissions.
 */
@RestController
@RequestMapping("/applications")
public class CreditApplicationController {

    private final CreditApplicationService creditApplicationService;

    public CreditApplicationController(CreditApplicationService creditApplicationService) {
        this.creditApplicationService = creditApplicationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateApplicationResponse create(@RequestBody CreateApplicationRequest request) {
        CreditApplication application = creditApplicationService.createApplication(
                request.getClientName(),
                request.getAmount()
        );
        return new CreateApplicationResponse(
                application.getId(),
                application.getClientName(),
                application.getAmount(),
                application.getStatus().name(),
                application.getCreatedAt()
        );
    }
}
