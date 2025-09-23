package com.litemax.ECoPro.dto.auth;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BulkUserOperationRequest {
    
    @NotEmpty(message = "User IDs cannot be empty")
    @Size(max = 100, message = "Cannot process more than 100 users at once")
    private List<Long> userIds;
    
    @NotBlank(message = "Operation is required")
    private String operation; // ACTIVATE, DEACTIVATE, BAN, LOCK, DELETE, ASSIGN_ROLE, REMOVE_ROLE
    
    private String reason;
    
    private String roleName; // Required for ASSIGN_ROLE and REMOVE_ROLE operations
    
    private boolean forceOperation = false; // For overriding certain validations
}