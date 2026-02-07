package com.example.credit;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/credit-applications")
public class CreditApplicationController {
    private static final Logger log = LoggerFactory.getLogger(CreditApplicationController.class);
    private final KafkaTemplate<String, CreditApplicationEvent> kafkaTemplate;

    public CreditApplicationController(KafkaTemplate<String, CreditApplicationEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CreditApplicationEvent submit(@RequestBody CreditApplicationRequest request) {
        CreditApplicationEvent event = new CreditApplicationEvent(
                UUID.randomUUID().toString(),
                request.clientName(),
                request.amount()
        );
        log.info("Publishing credit application event: id={}, clientName={}, amount={}",
                event.applicationId(), event.clientName(), event.amount());
        kafkaTemplate.send("credit-applications", event.applicationId(), event);
        return event;
    }
}
