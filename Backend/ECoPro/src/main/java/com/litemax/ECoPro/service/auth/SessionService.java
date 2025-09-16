package com.litemax.ECoPro.service.auth;

import com.litemax.ECoPro.entity.auth.Session;
import com.litemax.ECoPro.entity.auth.User;
import com.litemax.ECoPro.repository.auth.SessionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SessionService {

    private final SessionRepository sessionRepository;

    public void createSession(User user, String accessToken, String refreshToken, HttpServletRequest request) {
        log.debug("Creating session for user: {}", user.getEmail());

        Session session = new Session();
        session.setId(UUID.randomUUID().toString());
        session.setUser(user);
        session.setRefreshToken(refreshToken);
        session.setDeviceInfo(extractDeviceInfo(request));
        session.setIpAddress(extractClientIpAddress(request));
        session.setUserAgent(request.getHeader("User-Agent"));
        session.setExpiresAt(LocalDateTime.now().plusHours(24)); // 24 hours
        session.setRefreshExpiresAt(LocalDateTime.now().plusDays(30)); // 30 days
        session.setActive(true);

        sessionRepository.save(session);
        log.debug("Session created with ID: {}", session.getId());
    }

    public void updateSession(String oldRefreshToken, String newAccessToken, String newRefreshToken) {
        log.debug("Updating session with new tokens");

        Session session = sessionRepository.findByRefreshTokenAndActiveTrue(oldRefreshToken)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setRefreshToken(newRefreshToken);
        session.setExpiresAt(LocalDateTime.now().plusHours(24));
        session.setRefreshExpiresAt(LocalDateTime.now().plusDays(30));

        sessionRepository.save(session);
        log.debug("Session updated successfully");
    }

    public void invalidateSession(String accessToken) {
        // Since we don't store access token directly, we'll need to extract user info
        // and invalidate based on other criteria or implement a different approach
        log.debug("Invalidating session");
        // Implementation depends on your specific requirements
    }

    public void invalidateAllUserSessions(Long userId) {
        log.info("Invalidating all sessions for user ID: {}", userId);
        
        sessionRepository.findByUserIdAndActiveTrue(userId)
                .forEach(session -> {
                    session.setActive(false);
                    sessionRepository.save(session);
                });
        
        log.info("All sessions invalidated for user ID: {}", userId);
    }

    private String extractDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) return "Unknown";

        if (userAgent.contains("Mobile")) return "Mobile";
        if (userAgent.contains("Tablet")) return "Tablet";
        return "Desktop";
    }

    private String extractClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null) {
            return request.getRemoteAddr();
        } else {
            return xForwardedForHeader.split(",")[0];
        }
    }
}