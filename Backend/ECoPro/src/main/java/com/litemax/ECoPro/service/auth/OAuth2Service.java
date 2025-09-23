package com.litemax.ECoPro.service.auth;

import com.litemax.ECoPro.dto.auth.OAuth2LoginResponse;
import com.litemax.ECoPro.dto.auth.OAuth2ProviderDto;
import com.litemax.ECoPro.entity.auth.AuthProvider;
import com.litemax.ECoPro.entity.auth.User;
import com.litemax.ECoPro.exception.ResourceNotFoundException;
import com.litemax.ECoPro.exception.ValidationException;
import com.litemax.ECoPro.repository.auth.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OAuth2Service {

    private final UserRepository userRepository;

    @Value("${app.oauth2.base-url}")
    private String oauth2BaseUrl;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public String generateAuthorizationUrl(String provider, String redirectUri, HttpServletRequest request) {
        log.debug("Generating OAuth2 authorization URL for provider: {}", provider);

        String baseUrl = getBaseUrl(request);
        String finalRedirectUri = redirectUri != null ? redirectUri : frontendUrl;
        String state = UUID.randomUUID().toString();

        // Store state and redirect URI in session or cache for validation
        // This is a simplified version - in production, use proper state management
        
        return String.format("%s/oauth2/authorization/%s?redirect_uri=%s&state=%s", 
                baseUrl, provider.toLowerCase(), finalRedirectUri, state);
    }

    public void unlinkProvider(Long userId, String providerName) {
        log.info("Unlinking OAuth2 provider {} for user: {}", providerName, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        AuthProvider.ProviderType providerType;
        try {
            providerType = AuthProvider.ProviderType.valueOf(providerName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid provider: " + providerName);
        }

        // Check if user has a password or other authentication method
        boolean hasPassword = user.getPassword() != null && !user.getPassword().isEmpty();
        long otherProvidersCount = user.getAuthProviders().stream()
                .filter(provider -> provider.getProvider() != providerType)
                .count();

        if (!hasPassword && otherProvidersCount == 0) {
            throw new ValidationException("Cannot unlink the last authentication method. Please set a password first.");
        }

        // Remove the provider
        user.getAuthProviders().removeIf(provider -> provider.getProvider() == providerType);
        userRepository.save(user);

        log.info("OAuth2 provider {} successfully unlinked from user: {}", providerName, userId);
    }

    public OAuth2LoginResponse getLinkedProviders(Long userId) {
        log.debug("Getting linked providers for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        List<OAuth2ProviderDto> linkedProviders = user.getAuthProviders().stream()
                .map(this::convertToProviderDto)
                .collect(Collectors.toList());

        return OAuth2LoginResponse.builder()
                .userId(userId)
                .email(user.getEmail())
                .hasPassword(user.getPassword() != null && !user.getPassword().isEmpty())
                .linkedProviders(linkedProviders)
                .totalProviders(linkedProviders.size())
                .canUnlinkAll(user.getPassword() != null && !user.getPassword().isEmpty())
                .build();
    }

    public void linkProvider(Long userId, String providerName, String providerId, String email, String name, String imageUrl) {
        log.info("Linking OAuth2 provider {} to user: {}", providerName, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        AuthProvider.ProviderType providerType;
        try {
            providerType = AuthProvider.ProviderType.valueOf(providerName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid provider: " + providerName);
        }

        // Check if provider is already linked
        boolean alreadyLinked = user.getAuthProviders().stream()
                .anyMatch(provider -> provider.getProvider() == providerType);

        if (alreadyLinked) {
            throw new ValidationException("Provider " + providerName + " is already linked to this account");
        }

        // Create new auth provider
        AuthProvider authProvider = new AuthProvider();
        authProvider.setUser(user);
        authProvider.setProvider(providerType);
        authProvider.setProviderId(providerId);
        authProvider.setEmail(email);
        authProvider.setName(name);
        authProvider.setProfileImage(imageUrl);

        user.getAuthProviders().add(authProvider);
        userRepository.save(user);

        log.info("OAuth2 provider {} successfully linked to user: {}", providerName, userId);
    }

    private OAuth2ProviderDto convertToProviderDto(AuthProvider authProvider) {
        return OAuth2ProviderDto.builder()
                .provider(authProvider.getProvider().toString())
                .providerId(authProvider.getProviderId())
                .email(authProvider.getEmail())
                .name(authProvider.getName())
                .profileImage(authProvider.getProfileImage())
                .linkedAt(authProvider.getCreatedAt())
                .build();
    }

    private String getBaseUrl(HttpServletRequest request) {
        return String.format("%s://%s:%d", 
                request.getScheme(), 
                request.getServerName(), 
                request.getServerPort());
    }
}