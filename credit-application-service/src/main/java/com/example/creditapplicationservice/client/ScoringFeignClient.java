package com.example.creditapplicationservice.client;

import com.example.creditapplicationservice.dto.ScoringDecisionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "scoringFeignClient", url = "${downstream.scoring.base-url}")
public interface ScoringFeignClient {

    @GetMapping("/api/v1/scoring/decision")
    ScoringDecisionResponse getDecision(@RequestParam("clientName") String clientName);
}
