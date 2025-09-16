package com.litemax.ECoPro.entity.auth;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_providers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProviderType provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    private String email;
    private String name;
    private String profileImage;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum ProviderType {
        LOCAL, GOOGLE, FACEBOOK, GITHUB
    }
}
