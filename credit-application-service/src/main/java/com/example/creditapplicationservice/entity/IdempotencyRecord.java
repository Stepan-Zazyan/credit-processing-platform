package com.example.creditapplicationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "idempotency_records")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyRecord {
    @Id
    @Column(name = "key", nullable = false, length = 255)
    private String key;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "response_body", nullable = false, columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public IdempotencyRecord(String key, String requestHash, String responseBody, Integer status) {
        this.key = key;
        this.requestHash = requestHash;
        this.responseBody = responseBody;
        this.status = status;
    }
}
