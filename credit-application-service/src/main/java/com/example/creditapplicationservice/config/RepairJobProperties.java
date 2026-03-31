package com.example.creditapplicationservice.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "credit.application.repair")
public class RepairJobProperties {
    private Duration threshold = Duration.ofMinutes(2);
    private long pollingIntervalMs = 30000;
    private boolean checkStuckApplications = true;

    public Duration getThreshold() {
        return threshold;
    }

    public void setThreshold(Duration threshold) {
        this.threshold = threshold;
    }

    public long getPollingIntervalMs() {
        return pollingIntervalMs;
    }

    public void setPollingIntervalMs(long pollingIntervalMs) {
        this.pollingIntervalMs = pollingIntervalMs;
    }

    public boolean isCheckStuckApplications() {
        return checkStuckApplications;
    }

    public void setCheckStuckApplications(boolean checkStuckApplications) {
        this.checkStuckApplications = checkStuckApplications;
    }
}
