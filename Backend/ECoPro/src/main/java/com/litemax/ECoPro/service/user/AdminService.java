package com.litemax.ECoPro.service.user;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageImpl;

import com.litemax.ECoPro.dto.auth.ActivityAnalyticsDto;
import com.litemax.ECoPro.dto.auth.AddressDto;
import com.litemax.ECoPro.dto.auth.AdminActionResponse;
import com.litemax.ECoPro.dto.auth.AdminAnalyticsResponse;
import com.litemax.ECoPro.dto.auth.AdminAuditLogDto;
import com.litemax.ECoPro.dto.auth.AdminAuditLogFilterRequest;
import com.litemax.ECoPro.dto.auth.AdminDashboardResponse;
import com.litemax.ECoPro.dto.auth.BulkOperationItemResult;
import com.litemax.ECoPro.dto.auth.BulkOperationResponse;
import com.litemax.ECoPro.dto.auth.BulkUserOperationRequest;
import com.litemax.ECoPro.dto.auth.DailyStatisticDto;
import com.litemax.ECoPro.dto.auth.GeographicDistributionDto;
import com.litemax.ECoPro.dto.auth.GeographyAnalyticsDto;
import com.litemax.ECoPro.dto.auth.GrowthTrendDto;
import com.litemax.ECoPro.dto.auth.RecentActivityDto;
import com.litemax.ECoPro.dto.auth.RegistrationAnalyticsDto;
import com.litemax.ECoPro.dto.auth.StatisticItemDto;
import com.litemax.ECoPro.dto.auth.SystemHealthDto;
import com.litemax.ECoPro.dto.auth.SystemMemoryDto;
import com.litemax.ECoPro.dto.auth.SystemStatisticsResponse;
import com.litemax.ECoPro.dto.auth.UserActivitySummaryDto;
import com.litemax.ECoPro.dto.auth.UserAnalyticsDto;
import com.litemax.ECoPro.dto.auth.UserDetailResponse;
import com.litemax.ECoPro.dto.auth.UserStatisticsResponse;
import com.litemax.ECoPro.dto.auth.UserResponse;
import com.litemax.ECoPro.entity.auth.Address;
import com.litemax.ECoPro.entity.auth.Role;
import com.litemax.ECoPro.entity.auth.User;
import com.litemax.ECoPro.entity.auth.User.UserStatus;
import com.litemax.ECoPro.exception.ResourceNotFoundException;
import com.litemax.ECoPro.exception.ValidationException;
import com.litemax.ECoPro.repository.auth.AddressRepository;
import com.litemax.ECoPro.repository.auth.RoleRepository;
import com.litemax.ECoPro.repository.auth.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AddressRepository addressRepository;

    // Dashboard and Statistics

    public AdminDashboardResponse getDashboardData() {
        log.debug("Generating admin dashboard data");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        LocalDateTime twentyFourHoursAgo = now.minusHours(24);

        // User statistics
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(User.UserStatus.ACTIVE);
        long inactiveUsers = userRepository.countByStatus(User.UserStatus.INACTIVE);
        long bannedUsers = userRepository.countByStatus(User.UserStatus.BANNED);
        long lockedUsers = userRepository.countByStatus(User.UserStatus.LOCKED);

        // Recent activities
        long recentRegistrations = userRepository.countByCreatedAtAfter(thirtyDaysAgo);
        long recentLogins = userRepository.countByLastLoginAtAfter(sevenDaysAgo);
        long todayRegistrations = userRepository.countByCreatedAtAfter(twentyFourHoursAgo);

        // Growth calculations
        double userGrowthRate = calculateUserGrowthRate();
        
        // Role distribution
        Map<String, Long> roleDistribution = getUserCountByRole();
        
        // Recent activities
        List<RecentActivityDto> recentActivities = getRecentActivities(10);
        
        // System health
        SystemHealthDto systemHealth = getSystemHealthMetrics();

        // Top statistics
        List<StatisticItemDto> topStats = Arrays.asList(
            StatisticItemDto.builder().label("Total Users").value(totalUsers).build(),
            StatisticItemDto.builder().label("Active Users").value(activeUsers).build(),
            StatisticItemDto.builder().label("New This Month").value(recentRegistrations).build(),
            StatisticItemDto.builder().label("Login This Week").value(recentLogins).build()
        );

        log.debug("Dashboard data generated - Total Users: {}, Active: {}, Recent Registrations: {}", 
                totalUsers, activeUsers, recentRegistrations);

        return AdminDashboardResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .inactiveUsers(inactiveUsers)
                .bannedUsers(bannedUsers)
                .lockedUsers(lockedUsers)
                .recentRegistrations(recentRegistrations)
                .recentLogins(recentLogins)
                .todayRegistrations(todayRegistrations)
                .userGrowthRate(userGrowthRate)
                .roleDistribution(roleDistribution)
                .recentActivities(recentActivities)
                .systemHealth(systemHealth)
                .topStatistics(topStats)
                .generatedAt(now)
                .build();
    }

    public AdminAnalyticsResponse getAnalytics(String period, String category) {
        log.debug("Generating analytics for period: {}, category: {}", period, category);

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = getStartDateByPeriod(period, endDate);

        AdminAnalyticsResponse.AdminAnalyticsResponseBuilder builder = AdminAnalyticsResponse.builder()
                .period(period)
                .category(category)
                .startDate(startDate)
                .endDate(endDate);

        switch (category.toUpperCase()) {
            case "USERS":
                return builder
                        .userAnalytics(getUserAnalytics(startDate, endDate))
                        .build();
            case "REGISTRATIONS":
                return builder
                        .registrationAnalytics(getRegistrationAnalytics(startDate, endDate))
                        .build();
            case "ACTIVITY":
                return builder
                        .activityAnalytics(getActivityAnalytics(startDate, endDate))
                        .build();
            case "GEOGRAPHY":
                return builder
                        .geographyAnalytics(getGeographyAnalytics())
                        .build();
            default:
                return builder
                        .userAnalytics(getUserAnalytics(startDate, endDate))
                        .registrationAnalytics(getRegistrationAnalytics(startDate, endDate))
                        .build();
        }
    }

    // User Management

    public Page<UserResponse> getAllUsers(int page, int size, String search, String status, 
                                                String role, String sortBy, String sortDir) {
        
        log.debug("Fetching users - page: {}, size: {}, search: '{}', status: '{}', role: '{}'", 
                page, size, search, status, role);

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<User> users = buildUserQuery(search, status, role, pageable);

        log.debug("Found {} users out of {} total", users.getNumberOfElements(), users.getTotalElements());

        return users.map(this::convertToUserResponse);
    }

    public UserDetailResponse getUserById(Long userId) {
        log.debug("Fetching detailed user information for ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        // Get user's addresses
        List<Address> addresses = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
        
        // Get user activity summary
        UserActivitySummaryDto activitySummary = getUserActivitySummary(userId);
        
        return UserDetailResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .profileImage(user.getProfileImage())
                .status(user.getStatus())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .addresses(addresses.stream().map(this::convertToAddressDto).collect(Collectors.toList()))
                .activitySummary(activitySummary)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    public AdminActionResponse updateUserStatus(Long userId, String status, String reason, String adminEmail) {
        log.info("Admin {} updating user {} status to: {} with reason: {}", 
                adminEmail, userId, status, reason);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Validate status
        User.UserStatus newStatus;
        try {
            newStatus = User.UserStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid status: " + status + 
                    ". Valid statuses: " + Arrays.toString(User.UserStatus.values()));
        }

        // Business rules validation
        validateStatusChange(user, newStatus, adminEmail);

        User.UserStatus oldStatus = user.getStatus();
        user.setStatus(newStatus);
        userRepository.save(user);

        // Create audit log
        AdminAuditLogDto auditLog = createAuditLog("USER_STATUS_CHANGE", 
                String.format("Status changed from %s to %s", oldStatus, newStatus), 
                reason, adminEmail, userId);

        log.info("User {} status updated from {} to {} by admin: {}", 
                userId, oldStatus, newStatus, adminEmail);

        return AdminActionResponse.builder()
                .success(true)
                .message("User status updated successfully")
                .actionType("STATUS_UPDATE")
                .targetUserId(userId)
                .performedBy(adminEmail)
                .timestamp(LocalDateTime.now())
                .details(Map.of(
                    "oldStatus", oldStatus.toString(),
                    "newStatus", newStatus.toString(),
                    "reason", reason != null ? reason : "No reason provided"
                ))
                .auditLog(auditLog)
                .build();
    }

    public AdminActionResponse deleteUser(Long userId, String reason, String adminEmail) {
        log.warn("Admin {} attempting to delete user: {} with reason: {}", 
                adminEmail, userId, reason);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Business rules validation
        validateUserDeletion(user, adminEmail);

        User.UserStatus oldStatus = user.getStatus();
        user.setStatus(User.UserStatus.INACTIVE);
        userRepository.save(user);

        // Create audit log
        AdminAuditLogDto auditLog = createAuditLog("USER_DELETION", 
                "User account deactivated (soft delete)", reason, adminEmail, userId);

        log.warn("User {} soft deleted (status changed to INACTIVE) by admin: {}", userId, adminEmail);

        return AdminActionResponse.builder()
                .success(true)
                .message("User deleted successfully")
                .actionType("USER_DELETION")
                .targetUserId(userId)
                .performedBy(adminEmail)
                .timestamp(LocalDateTime.now())
                .details(Map.of(
                    "deletionType", "SOFT_DELETE",
                    "oldStatus", oldStatus.toString(),
                    "reason", reason != null ? reason : "No reason provided"
                ))
                .auditLog(auditLog)
                .build();
    }

    // Role Management

    public AdminActionResponse assignRole(Long userId, String roleName, String adminEmail) {
        log.info("Admin {} assigning role {} to user {}", adminEmail, roleName, userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        Role role = roleRepository.findByName(roleName.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        // Business rules validation
        validateRoleAssignment(user, role, adminEmail);

        user.addRole(role);
        userRepository.save(user);

        // Create audit log
        AdminAuditLogDto auditLog = createAuditLog("ROLE_ASSIGNMENT", 
                String.format("Role %s assigned to user", roleName), null, adminEmail, userId);

        log.info("Role {} assigned to user {} by admin: {}", roleName, userId, adminEmail);

        return AdminActionResponse.builder()
                .success(true)
                .message("Role assigned successfully")
                .actionType("ROLE_ASSIGNMENT")
                .targetUserId(userId)
                .performedBy(adminEmail)
                .timestamp(LocalDateTime.now())
                .details(Map.of(
                    "roleName", roleName,
                    "action", "ASSIGNED"
                ))
                .auditLog(auditLog)
                .build();
    }

    public AdminActionResponse removeRole(Long userId, String roleName, String adminEmail) {
        log.info("Admin {} removing role {} from user {}", adminEmail, roleName, userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        Role role = roleRepository.findByName(roleName.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        // Business rules validation
        validateRoleRemoval(user, role, adminEmail);

        user.removeRole(role);
        userRepository.save(user);

        // Create audit log
        AdminAuditLogDto auditLog = createAuditLog("ROLE_REMOVAL", 
                String.format("Role %s removed from user", roleName), null, adminEmail, userId);

        log.info("Role {} removed from user {} by admin: {}", roleName, userId, adminEmail);

        return AdminActionResponse.builder()
                .success(true)
                .message("Role removed successfully")
                .actionType("ROLE_REMOVAL")
                .targetUserId(userId)
                .performedBy(adminEmail)
                .timestamp(LocalDateTime.now())
                .details(Map.of(
                    "roleName", roleName,
                    "action", "REMOVED"
                ))
                .auditLog(auditLog)
                .build();
    }

    // Bulk Operations

    public BulkOperationResponse bulkUpdateUsers(BulkUserOperationRequest request, String adminEmail) {
        log.info("Admin {} performing bulk operation: {} on {} users", 
                adminEmail, request.getOperation(), request.getUserIds().size());
        
        BulkOperationResponse response = BulkOperationResponse.builder()
                .operation(request.getOperation())
                .totalRequested(request.getUserIds().size())
                .successCount(0)
                .failedCount(0)
                .results(new ArrayList<>())
                .performedBy(adminEmail)
                .startTime(LocalDateTime.now())
                .build();

        for (Long userId : request.getUserIds()) {
            try {
                BulkOperationItemResult itemResult = processBulkOperation(userId, request, adminEmail);
                response.getResults().add(itemResult);
                
                if (itemResult.isSuccess()) {
                    response.setSuccessCount(response.getSuccessCount() + 1);
                } else {
                    response.setFailedCount(response.getFailedCount() + 1);
                }
                
            } catch (Exception e) {
                log.error("Failed to process user {} in bulk operation: {}", userId, e.getMessage());
                
                BulkOperationItemResult errorResult = BulkOperationItemResult.builder()
                        .userId(userId)
                        .success(false)
                        .message("Operation failed: " + e.getMessage())
                        .errorCode("PROCESSING_ERROR")
                        .build();
                
                response.getResults().add(errorResult);
                response.setFailedCount(response.getFailedCount() + 1);
            }
        }
        
        response.setEndTime(LocalDateTime.now());
        response.setDurationMs(java.time.Duration.between(response.getStartTime(), response.getEndTime()).toMillis());
        
        log.info("Bulk operation completed - Success: {}, Failed: {}, Duration: {}ms", 
                response.getSuccessCount(), response.getFailedCount(), response.getDurationMs());
        
        return response;
    }

    // Statistics and Reports

    public UserStatisticsResponse getUserStatistics(int days) {
        log.debug("Generating user statistics for last {} days", days);
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        LocalDateTime endDate = LocalDateTime.now();
        
        // Basic counts
        long totalUsers = userRepository.count();
        long newRegistrations = userRepository.countByCreatedAtAfter(startDate);
        long activeUsers = userRepository.countByLastLoginAtAfter(startDate);
        
        // Status distribution
        Map<User.UserStatus, Long> statusDistribution = Arrays.stream(User.UserStatus.values())
                .collect(Collectors.toMap(
                    status -> status,
                    status -> userRepository.countByStatus(status)
                ));
        
        // Role distribution
        Map<String, Long> roleDistribution = getUserCountByRole();
        
        // Daily registrations
        List<DailyStatisticDto> dailyRegistrations = getDailyRegistrations(startDate, endDate);
        
        // Growth trends
        GrowthTrendDto growthTrend = calculateGrowthTrend(startDate, endDate);
        
        return UserStatisticsResponse.builder()
                .period(days + " days")
                .startDate(startDate)
                .endDate(endDate)
                .totalUsers(totalUsers)
                .newRegistrations(newRegistrations)
                .activeUsers(activeUsers)
                .statusDistribution(statusDistribution)
                .roleDistribution(roleDistribution)
                .dailyRegistrations(dailyRegistrations)
                .growthTrend(growthTrend)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    public SystemStatisticsResponse getSystemStatistics() {
        log.debug("Generating system statistics");
        
        // Database statistics
        Map<String, Long> databaseStats = new HashMap<>();
        databaseStats.put("totalUsers", userRepository.count());
        databaseStats.put("totalRoles", roleRepository.count());
        databaseStats.put("totalAddresses", addressRepository.count());
        
        // Memory statistics
        Runtime runtime = Runtime.getRuntime();
        SystemMemoryDto memoryStats = SystemMemoryDto.builder()
                .totalMemory(runtime.totalMemory())
                .freeMemory(runtime.freeMemory())
                .usedMemory(runtime.totalMemory() - runtime.freeMemory())
                .maxMemory(runtime.maxMemory())
                .memoryUsagePercentage(((double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory()) * 100)
                .build();
        
        // System health
        SystemHealthDto systemHealth = getSystemHealthMetrics();
        
        // Geographic distribution
        List<GeographicDistributionDto> geographicDistribution = getGeographicDistribution();
        
        return SystemStatisticsResponse.builder()
                .databaseStatistics(databaseStats)
                .memoryStatistics(memoryStats)
                .systemHealth(systemHealth)
                .geographicDistribution(geographicDistribution)
                .systemUptime(getSystemUptime())
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // Audit and Activity Logs

    public Page<AdminAuditLogDto> getAuditLogs(AdminAuditLogFilterRequest filter, Pageable pageable) {
        log.debug("Fetching audit logs with filters: {}", filter);
        
        // This would typically query an audit log table
        // For now, returning mock data
        List<AdminAuditLogDto> mockLogs = generateMockAuditLogs();
        
        // Apply filters
        List<AdminAuditLogDto> filteredLogs = mockLogs.stream()
                .filter(log -> applyAuditLogFilters(log, filter))
                .collect(Collectors.toList());
        
        // Convert to page
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredLogs.size());
        List<AdminAuditLogDto> pageContent = filteredLogs.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, filteredLogs.size());
    }

    // Private Helper Methods

    private Page<User> buildUserQuery(String search, String status, String role, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            if (status != null && !status.trim().isEmpty()) {
                User.UserStatus userStatus = User.UserStatus.valueOf(status.toUpperCase());
                return userRepository.findByEmailContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseAndStatus(
                        search.trim(), search.trim(), search.trim(), userStatus, pageable);
            } else {
                return userRepository.findByEmailContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                        search.trim(), search.trim(), search.trim(), pageable);
            }
        } else if (status != null && !status.trim().isEmpty()) {
            User.UserStatus userStatus = User.UserStatus.valueOf(status.toUpperCase());
            return userRepository.findByStatus(userStatus, pageable);
        } else if (role != null && !role.trim().isEmpty()) {
            return userRepository.findByRolesName(role.toUpperCase(), pageable);
        } else {
            return userRepository.findAll(pageable);
        }
    }

    private LocalDateTime getStartDateByPeriod(String period, LocalDateTime endDate) {
        switch (period.toUpperCase()) {
            case "7D":
            case "WEEK":
                return endDate.minusDays(7);
            case "30D":
            case "MONTH":
                return endDate.minusDays(30);
            case "90D":
            case "QUARTER":
                return endDate.minusDays(90);
            case "365D":
            case "YEAR":
                return endDate.minusDays(365);
            default:
                return endDate.minusDays(30);
        }
    }

    private double calculateUserGrowthRate() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastMonth = now.minusDays(30);
        LocalDateTime previousMonth = now.minusDays(60);
        
        long currentPeriodUsers = userRepository.countByCreatedAtBetween(lastMonth, now);
        long previousPeriodUsers = userRepository.countByCreatedAtBetween(previousMonth, lastMonth);
        
        if (previousPeriodUsers == 0) {
            return currentPeriodUsers > 0 ? 100.0 : 0.0;
        }
        
        return ((double) (currentPeriodUsers - previousPeriodUsers) / previousPeriodUsers) * 100.0;
    }

    private Map<String, Long> getUserCountByRole() {
        Map<String, Long> roleDistribution = new HashMap<>();
        List<Role> roles = roleRepository.findAll();
        
        for (Role role : roles) {
            long count = userRepository.countByRolesContaining(role);
            roleDistribution.put(role.getName(), count);
        }
        
        return roleDistribution;
    }

    private List<RecentActivityDto> getRecentActivities(int limit) {
        // Mock implementation - would typically query audit logs
        List<RecentActivityDto> activities = new ArrayList<>();
        
        activities.add(RecentActivityDto.builder()
                .id(1L)
                .action("USER_REGISTERED")
                .description("New user john.doe@example.com registered")
                .userEmail("john.doe@example.com")
                .ipAddress("192.168.1.1")
                .timestamp(LocalDateTime.now().minusHours(1))
                .build());
                
        activities.add(RecentActivityDto.builder()
                .id(2L)
                .action("USER_LOGIN")
                .description("User jane.smith@example.com logged in")
                .userEmail("jane.smith@example.com")
                .ipAddress("192.168.1.2")
                .timestamp(LocalDateTime.now().minusHours(2))
                .build());
                
        return activities;
    }

    private SystemHealthDto getSystemHealthMetrics() {
        Map<String, String> healthChecks = new HashMap<>();
        
        // Database health
        try {
            userRepository.count();
            healthChecks.put("database", "HEALTHY");
        } catch (Exception e) {
            healthChecks.put("database", "UNHEALTHY");
        }
        
        // Memory health
        Runtime runtime = Runtime.getRuntime();
        double memoryUsage = (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory();
        healthChecks.put("memory", memoryUsage > 0.9 ? "HIGH_USAGE" : "HEALTHY");
        
        // Overall status
        boolean allHealthy = healthChecks.values().stream().allMatch("HEALTHY"::equals);
        String overallStatus = allHealthy ? "HEALTHY" : (healthChecks.values().contains("UNHEALTHY") ? "UNHEALTHY" : "WARNING");
        
        return SystemHealthDto.builder()
                .overallStatus(overallStatus)
                .healthChecks(healthChecks)
                .lastChecked(LocalDateTime.now())
                .build();
    }

    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .profileImage(user.getProfileImage())
                .status(user.getStatus().toString())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .roles(user.getRoles().stream().map(Role::getName).toArray(String[]::new))
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    private AddressDto convertToAddressDto(Address address) {
        return AddressDto.builder()
                .id(address.getId())
                .fullName(address.getFullName())
                .phone(address.getPhone())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .country(address.getCountry())
                .type(address.getType())
                .isDefault(address.isDefault())
                .formattedAddress(address.getFormattedAddress())
                .createdAt(address.getCreatedAt())
                .build();
    }

    // Additional helper methods for analytics, validation, etc.
    // ... (implementation continues with remaining helper methods)
    private BulkOperationItemResult processBulkOperation(Long userId, BulkUserOperationRequest request, String adminEmail) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return BulkOperationItemResult.builder()
                        .userId(userId)
                        .success(false)
                        .message("User not found")
                        .errorCode("USER_NOT_FOUND")
                        .processedAt(LocalDateTime.now())
                        .build();
            }

            switch (request.getOperation().toUpperCase()) {
                case "ACTIVATE":
                    updateUserStatus(userId, "ACTIVE", request.getReason(), adminEmail);
                    break;
                case "DEACTIVATE":
                    updateUserStatus(userId, "INACTIVE", request.getReason(), adminEmail);
                    break;
                case "BAN":
                    updateUserStatus(userId, "BANNED", request.getReason(), adminEmail);
                    break;
                case "LOCK":
                    updateUserStatus(userId, "LOCKED", request.getReason(), adminEmail);
                    break;
                case "DELETE":
                    deleteUser(userId, request.getReason(), adminEmail);
                    break;
                case "ASSIGN_ROLE":
                    if (request.getRoleName() == null) {
                        throw new ValidationException("Role name is required for ASSIGN_ROLE operation");
                    }
                    assignRole(userId, request.getRoleName(), adminEmail);
                    break;
                case "REMOVE_ROLE":
                    if (request.getRoleName() == null) {
                        throw new ValidationException("Role name is required for REMOVE_ROLE operation");
                    }
                    removeRole(userId, request.getRoleName(), adminEmail);
                    break;
                default:
                    throw new ValidationException("Invalid operation: " + request.getOperation());
            }

            return BulkOperationItemResult.builder()
                    .userId(userId)
                    .userEmail(user.getEmail())
                    .success(true)
                    .message("Operation completed successfully")
                    .details(Map.of("operation", request.getOperation(), "reason", request.getReason() != null ? request.getReason() : ""))
                    .processedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            return BulkOperationItemResult.builder()
                    .userId(userId)
                    .success(false)
                    .message(e.getMessage())
                    .errorCode("OPERATION_FAILED")
                    .processedAt(LocalDateTime.now())
                    .build();
        }
    }

    private AdminAuditLogDto createAuditLog(String action, String description, String reason, String performedBy, Long targetUserId) {
        return AdminAuditLogDto.builder()
                .action(action)
                .description(description)
                .reason(reason)
                .performedBy(performedBy)
                .targetUserId(targetUserId)
                .ipAddress("127.0.0.1") // Would get from request context
                .severity("INFO")
                .timestamp(LocalDateTime.now())
                .metadata(Map.of("source", "ADMIN_PANEL"))
                .build();
    }

    private void validateStatusChange(User user, User.UserStatus newStatus, String adminEmail) {
        if (user.getEmail().equals(adminEmail)) {
            throw new ValidationException("Cannot change your own status");
        }
        
        if (user.hasRole("ADMIN") && newStatus != User.UserStatus.ACTIVE && !isSuperAdmin(adminEmail)) {
            throw new ValidationException("Only super administrators can change admin user status");
        }
    }

    private void validateUserDeletion(User user, String adminEmail) {
        if (user.hasRole("ADMIN")) {
            throw new ValidationException("Cannot delete admin users");
        }
        
        if (user.getEmail().equals(adminEmail)) {
            throw new ValidationException("Cannot delete your own account");
        }
    }

    private void validateRoleAssignment(User user, Role role, String adminEmail) {
        if (user.getRoles().contains(role)) {
            throw new ValidationException("User already has role: " + role.getName());
        }
        
        if ("ADMIN".equals(role.getName()) && !isSuperAdmin(adminEmail)) {
            throw new ValidationException("Only super administrators can assign ADMIN role");
        }
    }

    private void validateRoleRemoval(User user, Role role, String adminEmail) {
        if (!user.getRoles().contains(role)) {
            throw new ValidationException("User does not have role: " + role.getName());
        }
        
        if (user.getRoles().size() <= 1) {
            throw new ValidationException("Cannot remove the last role from user");
        }
        
        if ("ADMIN".equals(role.getName()) && user.getEmail().equals(adminEmail)) {
            throw new ValidationException("Cannot remove ADMIN role from your own account");
        }
    }

    private boolean isSuperAdmin(String adminEmail) {
        // Define super admin logic - could be based on email, special role, or configuration
        return "superadmin@ecommerce.com".equals(adminEmail);
    }

    private UserActivitySummaryDto getUserActivitySummary(Long userId) {
        // Mock implementation - would typically query activity/session tables
        return UserActivitySummaryDto.builder()
                .totalLogins(25L)
                .firstLogin(LocalDateTime.now().minusDays(90))
                .lastLogin(LocalDateTime.now().minusHours(2))
                .sessionsThisMonth(12L)
                .averageSessionDuration(1800L) // 30 minutes in seconds
                .mostActiveDay("Monday")
                .mostActiveHour("14:00")
                .totalOrders(5L)
                .totalSpent(299.99)
                .accountAge("3 months")
                .isActiveUser(true)
                .build();
    }

    private List<DailyStatisticDto> getDailyRegistrations(LocalDateTime startDate, LocalDateTime endDate) {
        // Mock implementation - would typically query registration data grouped by day
        List<DailyStatisticDto> dailyStats = new ArrayList<>();
        LocalDate current = startDate.toLocalDate();
        LocalDate end = endDate.toLocalDate();
        
        while (!current.isAfter(end)) {
            long count = (long) (Math.random() * 10) + 1; // Mock data
            dailyStats.add(DailyStatisticDto.builder()
                    .date(current)
                    .count(count)
                    .label(current.toString())
                    .build());
            current = current.plusDays(1);
        }
        
        return dailyStats;
    }

    private GrowthTrendDto calculateGrowthTrend(LocalDateTime startDate, LocalDateTime endDate) {
        // Mock implementation - would calculate actual growth trends
        return GrowthTrendDto.builder()
                .currentPeriodGrowth(15.5)
                .previousPeriodGrowth(12.3)
                .growthRate(26.0)
                .trend("INCREASING")
                .interpretation("User registration is trending upward with a 26% growth rate")
                .build();
    }

    private List<GeographicDistributionDto> getGeographicDistribution() {
        // Mock implementation - would query address data grouped by country
        return Arrays.asList(
            GeographicDistributionDto.builder()
                    .country("United States")
                    .countryCode("US")
                    .userCount(1250L)
                    .percentage(45.5)
                    .build(),
            GeographicDistributionDto.builder()
                    .country("Canada")
                    .countryCode("CA")
                    .userCount(320L)
                    .percentage(11.6)
                    .build()
        );
    }

    private String getSystemUptime() {
        // Mock implementation - would calculate actual system uptime
        return "15 days, 8 hours, 42 minutes";
    }

    private List<AdminAuditLogDto> generateMockAuditLogs() {
        // Mock implementation for demonstration
        return Arrays.asList(
            AdminAuditLogDto.builder()
                    .id(1L)
                    .action("USER_STATUS_CHANGE")
                    .description("User status changed from ACTIVE to INACTIVE")
                    .performedBy("admin@ecommerce.com")
                    .targetUserId(123L)
                    .targetUserEmail("user@example.com")
                    .severity("INFO")
                    .timestamp(LocalDateTime.now().minusHours(1))
                    .build(),
            AdminAuditLogDto.builder()
                    .id(2L)
                    .action("ROLE_ASSIGNMENT")
                    .description("Role SELLER assigned to user")
                    .performedBy("admin@ecommerce.com")
                    .targetUserId(124L)
                    .targetUserEmail("seller@example.com")
                    .severity("INFO")
                    .timestamp(LocalDateTime.now().minusHours(2))
                    .build()
        );
    }

    private boolean applyAuditLogFilters(AdminAuditLogDto log, AdminAuditLogFilterRequest filter) {
        if (filter.getAction() != null && !log.getAction().equals(filter.getAction())) {
            return false;
        }
        if (filter.getPerformedBy() != null && !log.getPerformedBy().equals(filter.getPerformedBy())) {
            return false;
        }
        if (filter.getTargetUserId() != null && !filter.getTargetUserId().equals(log.getTargetUserId())) {
            return false;
        }
        if (filter.getSeverity() != null && !log.getSeverity().equals(filter.getSeverity())) {
            return false;
        }
        if (filter.getDateFrom() != null && log.getTimestamp().isBefore(filter.getDateFrom())) {
            return false;
        }
        if (filter.getDateTo() != null && log.getTimestamp().isAfter(filter.getDateTo())) {
            return false;
        }
        return true;
    }

    // Analytics helper methods
    private UserAnalyticsDto getUserAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        // Implementation would query actual data
        return UserAnalyticsDto.builder()
                .totalUsers(userRepository.count())
                .activeUsers(userRepository.countByLastLoginAtAfter(startDate))
                .newUsers(userRepository.countByCreatedAtBetween(startDate, endDate))
                .retentionRate(78.5)
                .churnRate(12.3)
                .build();
    }

    private RegistrationAnalyticsDto getRegistrationAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        // Implementation would analyze registration patterns
        return RegistrationAnalyticsDto.builder()
                .totalRegistrations(userRepository.countByCreatedAtBetween(startDate, endDate))
                .averageDaily(8.5)
                .peakRegistrationDay("Tuesday")
                .peakRegistrationHour("10:00")
                .build();
    }

    private ActivityAnalyticsDto getActivityAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        // Implementation would analyze user activity data
        return ActivityAnalyticsDto.builder()
                .totalSessions(2450L)
                .averageSessionDuration(1650.0)
                .uniqueActiveUsers(845L)
                .bounceRate(23.5)
                .build();
    }

    private GeographyAnalyticsDto getGeographyAnalytics() {
        // Implementation would analyze geographic data
        return GeographyAnalyticsDto.builder()
                .countries(getGeographicDistribution())
                .primaryMarket("United States")
                .marketConcentration(67.8)
                .build();
    }
}