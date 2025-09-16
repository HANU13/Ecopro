package com.litemax.ECoPro.dto.auth;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String profileImage;
    private String status;
    private boolean emailVerified;
    private boolean phoneVerified;
    private String[] roles;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}