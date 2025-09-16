package com.litemax.ECoPro.service.auth;

import com.litemax.ECoPro.dto.auth.*;
import com.litemax.ECoPro.entity.auth.EmailService;
import com.litemax.ECoPro.entity.auth.Role;
import com.litemax.ECoPro.entity.auth.User;
import com.litemax.ECoPro.exception.ResourceNotFoundException;
import com.litemax.ECoPro.exception.ValidationException;
import com.litemax.ECoPro.repository.auth.RoleRepository;
import com.litemax.ECoPro.repository.auth.UserRepository;
import com.litemax.ECoPro.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final SessionService sessionService;
    private final EmailService emailService;

    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getEmail());

        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed - user already exists: {}", request.getEmail());
            throw new ValidationException("Email already registered");
        }

        // Create new user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setStatus(User.UserStatus.ACTIVE);

        // Generate email verification token
        user.setEmailVerificationToken(UUID.randomUUID().toString());
        user.setEmailVerificationExpiry(LocalDateTime.now().plusHours(24));

        // Assign role based on request
        String roleName = request.getRole() != null ? request.getRole().toUpperCase() : "CUSTOMER";
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ValidationException("Invalid role: " + roleName));
        
        user.addRole(role);

        user = userRepository.save(user);
        log.info("User registered successfully with ID: {}", user.getId());

        // Send verification email
        try {
            emailService.sendEmailVerification(user);
            log.info("Verification email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", user.getEmail(), e);
        }

        return AuthResponse.builder()
                .message("Registration successful. Please check your email for verification.")
                .user(convertToUserResponse(user))
                .build();
    }

    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        log.info("Login attempt for user: {}", request.getEmail());

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            User user = (User) authentication.getPrincipal();
            log.info("User authenticated successfully: {}", user.getEmail());

            // Update last login
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            // Generate tokens
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId());
            claims.put("roles", user.getRoles().stream().map(Role::getName).toArray());

            String accessToken = jwtUtil.generateToken(user, claims);
            String refreshToken = jwtUtil.generateRefreshToken(user);

            // Create session
            sessionService.createSession(user, accessToken, refreshToken, httpRequest);

            log.info("Login successful for user: {}", user.getEmail());

            return AuthResponse.builder()
                    .message("Login successful")
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .user(convertToUserResponse(user))
                    .build();

        } catch (BadCredentialsException e) {
            log.warn("Login failed - invalid credentials for user: {}", request.getEmail());
            throw new ValidationException("Invalid email or password");
        }
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        
        log.debug("Processing refresh token request");

        if (!jwtUtil.isRefreshToken(refreshToken)) {
            throw new ValidationException("Invalid refresh token");
        }

        try {
            String email = jwtUtil.extractUsername(refreshToken);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if (jwtUtil.validateToken(refreshToken, user)) {
                // Generate new tokens
                Map<String, Object> claims = new HashMap<>();
                claims.put("userId", user.getId());
                claims.put("roles", user.getRoles().stream().map(Role::getName).toArray());

                String newAccessToken = jwtUtil.generateToken(user, claims);
                String newRefreshToken = jwtUtil.generateRefreshToken(user);

                // Update session
                sessionService.updateSession(refreshToken, newAccessToken, newRefreshToken);

                log.info("Tokens refreshed for user: {}", user.getEmail());

                return AuthResponse.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(newRefreshToken)
                        .user(convertToUserResponse(user))
                        .build();
            }
        } catch (Exception e) {
            log.error("Token refresh failed", e);
        }

        throw new ValidationException("Invalid or expired refresh token");
    }

    public void logout(String accessToken) {
        log.info("Processing logout request");
        
        try {
            String email = jwtUtil.extractUsername(accessToken);
            sessionService.invalidateSession(accessToken);
            log.info("User logged out successfully: {}", email);
        } catch (Exception e) {
            log.error("Logout processing failed", e);
        }
    }

    public AuthResponse verifyEmail(String token) {
        log.info("Processing email verification with token: {}", token);

        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new ValidationException("Invalid verification token"));

        if (user.getEmailVerificationExpiry().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Verification token has expired");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiry(null);
        userRepository.save(user);

        log.info("Email verified successfully for user: {}", user.getEmail());

        return AuthResponse.builder()
                .message("Email verified successfully")
                .user(convertToUserResponse(user))
                .build();
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        log.info("Processing forgot password request for: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        // Generate reset token
        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        // Send reset email
        try {
            emailService.sendPasswordResetEmail(user, resetToken);
            log.info("Password reset email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset email", e);
            throw new ValidationException("Failed to send reset email");
        }
    }

    public AuthResponse resetPassword(ResetPasswordRequest request) {
        log.info("Processing password reset");

        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new ValidationException("Invalid reset token"));

        if (user.getPasswordResetExpiry().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiry(null);
        userRepository.save(user);

        // Invalidate all sessions for security
        sessionService.invalidateAllUserSessions(user.getId());

        log.info("Password reset successfully for user: {}", user.getEmail());

        return AuthResponse.builder()
                .message("Password reset successfully")
                .user(convertToUserResponse(user))
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
}
