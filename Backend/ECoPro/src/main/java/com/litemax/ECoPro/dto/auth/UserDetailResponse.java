package com.litemax.ECoPro.dto.auth;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.litemax.ECoPro.entity.auth.User;

@Data
@Builder
public class UserDetailResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phone;
    private String profileImage;
    private User.UserStatus status;
    private boolean emailVerified;
    private boolean phoneVerified;
    private Set<String> roles;
    private List<AddressDto> addresses;
    private UserActivitySummaryDto activitySummary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
}