package com.litemax.ECoPro.controller.user;


import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.litemax.ECoPro.dto.auth.AdminAuditLogDto;
import com.litemax.ECoPro.dto.auth.AdminAuditLogFilterRequest;
import com.litemax.ECoPro.dto.auth.AdminDashboardResponse;
import com.litemax.ECoPro.dto.auth.BulkOperationResponse;
import com.litemax.ECoPro.dto.auth.BulkUserOperationRequest;
import com.litemax.ECoPro.dto.auth.SystemStatisticsResponse;
import com.litemax.ECoPro.dto.auth.UserDetailResponse;
import com.litemax.ECoPro.dto.auth.UserResponse;
import com.litemax.ECoPro.dto.auth.UserStatisticsResponse;
import com.litemax.ECoPro.service.user.AdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Management", description = "Admin-only APIs for system management")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    @Operation(
        summary = "Get admin dashboard data",
        description = "Retrieves key metrics and statistics for the admin dashboard"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dashboard data retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content)
    })
    public ResponseEntity<AdminDashboardResponse> getDashboardData(
            @AuthenticationPrincipal UserDetails adminUser) {
        
        log.info("Admin dashboard request from: {}", adminUser.getUsername());
        AdminDashboardResponse dashboard = adminService.getDashboardData();
        return ResponseEntity.ok(dashboard);
    }

    // User Management APIs
    @GetMapping("/users")
    @Operation(
        summary = "Get all users with pagination and filtering",
        description = "Retrieves paginated list of users with optional search and status filtering"
    )
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Search term for email, name", example = "john")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filter by user status", example = "ACTIVE")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by role", example = "CUSTOMER")
            @RequestParam(required = false) String role,
            @Parameter(description = "Sort by field", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDir,
            @AuthenticationPrincipal UserDetails adminUser) {
        
        log.info("Admin {} fetching users - page: {}, size: {}, search: {}, status: {}, role: {}", 
                adminUser.getUsername(), page, size, search, status, role);
        
        Page<UserResponse> users = adminService.getAllUsers(page, size, search, status, role, sortBy, sortDir);
        
        log.debug("Retrieved {} users out of {} total for admin: {}", 
                users.getNumberOfElements(), users.getTotalElements(), adminUser.getUsername());
        
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{userId}")
    @Operation(
        summary = "Get user details by ID",
        description = "Retrieves detailed information about a specific user"
    )
    public ResponseEntity<UserDetailResponse> getUserById(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails adminUser) {
        
        log.info("Admin {} fetching user details for ID: {}", adminUser.getUsername(), userId);
        UserDetailResponse user = adminService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/users/{userId}/status")
    @Operation(
        summary = "Update user status",
        description = "Updates the status of a user (ACTIVE, INACTIVE, LOCKED, BANNED)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User status updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid status provided", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<Void> updateUserStatus(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "New status", required = true, example = "INACTIVE")
            @RequestParam String status,
            @Parameter(description = "Reason for status change")
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserDetails adminUser) {
        
        log.info("Admin {} updating user {} status to: {} with reason: {}", 
                adminUser.getUsername(), userId, status, reason);
        
        adminService.updateUserStatus(userId, status, reason, adminUser.getUsername());
        
        log.info("User {} status updated to {} by admin: {}", userId, status, adminUser.getUsername());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{userId}")
    @Operation(
        summary = "Delete user (soft delete)",
        description = "Soft deletes a user by setting their status to INACTIVE"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "User deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content),
        @ApiResponse(responseCode = "400", description = "Cannot delete admin user", content = @Content)
    })
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "User ID to delete", required = true)
            @PathVariable Long userId,
            @Parameter(description = "Reason for deletion")
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserDetails adminUser) {
        
        log.warn("Admin {} attempting to delete user: {} with reason: {}", 
                adminUser.getUsername(), userId, reason);
        
        adminService.deleteUser(userId, reason, adminUser.getUsername());
        
        log.warn("User {} soft deleted by admin: {}", userId, adminUser.getUsername());
        return ResponseEntity.noContent().build();
    }

    // Role Management APIs
    @PostMapping("/users/{userId}/roles")
    @Operation(
        summary = "Assign role to user",
        description = "Assigns a new role to the specified user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Role assigned successfully"),
        @ApiResponse(responseCode = "400", description = "User already has this role or invalid role", content = @Content),
        @ApiResponse(responseCode = "404", description = "User or role not found", content = @Content)
    })
    public ResponseEntity<Void> assignRole(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "Role name to assign", required = true, example = "SELLER")
            @RequestParam String roleName,
            @AuthenticationPrincipal UserDetails adminUser) {
        
        log.info("Admin {} assigning role {} to user {}", adminUser.getUsername(), roleName, userId);
        adminService.assignRole(userId, roleName, adminUser.getUsername());
        log.info("Role {} assigned to user {} by admin: {}", roleName, userId, adminUser.getUsername());
        
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{userId}/roles")
    @Operation(
        summary = "Remove role from user",
        description = "Removes a role from the specified user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Role removed successfully"),
        @ApiResponse(responseCode = "400", description = "User doesn't have this role or cannot remove role", content = @Content),
        @ApiResponse(responseCode = "404", description = "User or role not found", content = @Content)
    })
    public ResponseEntity<Void> removeRole(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "Role name to remove", required = true, example = "SELLER")
            @RequestParam String roleName,
            @AuthenticationPrincipal UserDetails adminUser) {
        
        log.info("Admin {} removing role {} from user {}", adminUser.getUsername(), roleName, userId);
        adminService.removeRole(userId, roleName, adminUser.getUsername());
        log.info("Role {} removed from user {} by admin: {}", roleName, userId, adminUser.getUsername());
        
        return ResponseEntity.ok().build();
    }

    // System Statistics APIs
    @GetMapping("/stats/users")
    @Operation(
        summary = "Get user statistics",
        description = "Retrieves user registration and activity statistics"
    )
    public ResponseEntity<UserStatisticsResponse> getUserStats(
            @Parameter(description = "Number of days to include in stats", example = "30")
            @RequestParam(defaultValue = "30") int days,
            @AuthenticationPrincipal UserDetails adminUser) {
        
        log.debug("Admin {} fetching user stats for last {} days", adminUser.getUsername(), days);
        UserStatisticsResponse stats = adminService.getUserStatistics(days);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/system")
    @Operation(
        summary = "Get system statistics",
        description = "Retrieves overall system health and usage statistics"
    )
    public ResponseEntity<SystemStatisticsResponse> getSystemStats(
            @AuthenticationPrincipal UserDetails adminUser) {
        
        log.debug("Admin {} fetching system statistics", adminUser.getUsername());
        SystemStatisticsResponse stats = adminService.getSystemStatistics();
        return ResponseEntity.ok(stats);
    }

    // Bulk Operations
    @PostMapping("/users/bulk-update")
    @Operation(
        summary = "Bulk update users",
        description = "Performs bulk operations on multiple users"
    )
    public ResponseEntity<BulkOperationResponse> bulkUpdateUsers(
            @Parameter(description = "Bulk update request details", required = true)
            @Valid @RequestBody BulkUserOperationRequest request,
            @AuthenticationPrincipal UserDetails adminUser) {
        
        log.info("Admin {} performing bulk operation: {} on {} users", 
                adminUser.getUsername(), request.getOperation(), request.getUserIds().size());
        
        BulkOperationResponse result = adminService.bulkUpdateUsers(request, adminUser.getUsername());
        
        log.info("Bulk operation completed by admin: {} - Success: {}, Failed: {}", 
                adminUser.getUsername(), result.getSuccessCount(), result.getDurationMs());
        
        return ResponseEntity.ok(result);
    }

    // Activity Logs
//    @GetMapping("/activity-logs")
//    @Operation(
//        summary = "Get system activity logs",
//        description = "Retrieves system activity and audit logs with pagination"
//    )
//    public ResponseEntity<Page<Map<String, Object>>> getActivityLogs(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size,
//            @RequestParam(required = false) String action,
//            @RequestParam(required = false) Long userId,
//            @RequestParam(required = false) String dateFrom,
//            @RequestParam(required = false) String dateTo,
//            @AuthenticationPrincipal UserDetails adminUser) {
//        
//        log.debug("Admin {} fetching activity logs", adminUser.getUsername());
//        Page<Map<String, Object>> logs = adminService.getActivityLogs(page, size, action, userId, dateFrom, dateTo);
//        return ResponseEntity.ok(logs);
//    }
    @GetMapping("/activity-logs")
    @Operation(
        summary = "Get system activity logs",
        description = "Retrieves system activity and audit logs with pagination"
    )
    public ResponseEntity<Page<AdminAuditLogDto>> getActivityLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) LocalDateTime dateFrom,
            @RequestParam(required = false) LocalDateTime dateTo,
            @AuthenticationPrincipal UserDetails adminUser) {
        
        log.debug("Admin {} fetching activity logs", adminUser.getUsername());

        // Build filter request object
        AdminAuditLogFilterRequest filter = new AdminAuditLogFilterRequest();
        filter.setAction(action);
        filter.setPerformedBy(userId);
        filter.setDateFrom(dateFrom);
        filter.setDateTo(dateTo);

        // Create pageable
        Pageable pageable = PageRequest.of(page, size);

        // Call service
        Page<AdminAuditLogDto> logs = adminService.getAuditLogs(filter, pageable);

        return ResponseEntity.ok(logs);
    }

}