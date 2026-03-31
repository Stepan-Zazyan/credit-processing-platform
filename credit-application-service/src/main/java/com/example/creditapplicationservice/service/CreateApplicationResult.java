package com.example.creditapplicationservice.service;

import java.util.Map;

public class CreateApplicationResult {
    private final int status;
    private final Map<String, Object> body;

    public CreateApplicationResult(int status, Map<String, Object> body) {
        this.status = status;
        this.body = body;
    }

    public int getStatus() {
        return status;
    }

    public Map<String, Object> getBody() {
        return body;
    }
}
