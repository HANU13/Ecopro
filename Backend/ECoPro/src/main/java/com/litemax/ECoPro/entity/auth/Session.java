package com.litemax.ECoPro.entity.auth;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Session {

    @Id
    private String id; // JWT token ID

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String refreshToken;

    private String deviceInfo;
    private String ipAddress;
    private String userAgent;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;
    private LocalDateTime refreshExpiresAt;

    private boolean active = true;

    @Enumerated(EnumType.STRING)
    private SessionType type = SessionType.WEB;

    public enum SessionType {
        WEB, MOBILE, API
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isRefreshExpired() {
        return LocalDateTime.now().isAfter(refreshExpiresAt);
    }
}