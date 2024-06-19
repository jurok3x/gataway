package com.ykotsiuba.gateway_app.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
public class UserSession {

    @Id
    private String id;
    private String email;
    private String name;
    private Instant expiresAt;

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }
}
