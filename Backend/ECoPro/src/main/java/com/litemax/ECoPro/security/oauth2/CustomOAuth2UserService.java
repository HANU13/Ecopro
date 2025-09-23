package com.litemax.ECoPro.security.oauth2;

import com.litemax.ECoPro.entity.auth.AuthProvider;
import com.litemax.ECoPro.entity.auth.Role;
import com.litemax.ECoPro.entity.auth.User;
import com.litemax.ECoPro.exception.OAuth2AuthenticationProcessingException;
import com.litemax.ECoPro.repository.auth.RoleRepository;
import com.litemax.ECoPro.repository.auth.UserRepository;
import com.litemax.ECoPro.security.oauth2.user.OAuth2UserInfo;
import com.litemax.ECoPro.security.oauth2.user.OAuth2UserInfoFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (Exception ex) {
            log.error("Error processing OAuth2 user", ex);
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        log.info("Processing OAuth2 user from provider: {}", registrationId);

        if (!StringUtils.hasText(oAuth2UserInfo.getEmail())) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(oAuth2UserInfo.getEmail());
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            log.info("Existing user found: {}", user.getEmail());
            user = updateExistingUser(user, oAuth2UserInfo, registrationId);
        } else {
            log.info("Creating new user from OAuth2 provider: {}", oAuth2UserInfo.getEmail());
            user = registerNewUser(oAuth2UserRequest, oAuth2UserInfo);
        }

        return new CustomOAuth2User(user, oAuth2User.getAttributes());
    }

    private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo) {
        User user = new User();
        user.setEmail(oAuth2UserInfo.getEmail());
        user.setFirstName(oAuth2UserInfo.getFirstName());
        user.setLastName(oAuth2UserInfo.getLastName());
        user.setProfileImage(oAuth2UserInfo.getImageUrl());
        user.setEmailVerified(true);
        user.setStatus(User.UserStatus.ACTIVE);

        // Assign default CUSTOMER role
        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Customer role not found"));
        user.addRole(customerRole);

        user = userRepository.save(user);
        log.info("New user created with ID: {}", user.getId());

        // Create auth provider entry
        createAuthProvider(user, oAuth2UserRequest.getClientRegistration().getRegistrationId(), oAuth2UserInfo);

        return user;
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo, String registrationId) {
        existingUser.setFirstName(oAuth2UserInfo.getFirstName());
        existingUser.setLastName(oAuth2UserInfo.getLastName());
        existingUser.setProfileImage(oAuth2UserInfo.getImageUrl());
        existingUser.setEmailVerified(true);

        User updatedUser = userRepository.save(existingUser);
        log.info("User updated: {}", updatedUser.getEmail());

        // Update or create auth provider entry
        createOrUpdateAuthProvider(updatedUser, registrationId, oAuth2UserInfo);

        return updatedUser;
    }

    private void createAuthProvider(User user, String registrationId, OAuth2UserInfo oAuth2UserInfo) {
        AuthProvider authProvider = new AuthProvider();
        authProvider.setUser(user);
        authProvider.setProvider(AuthProvider.ProviderType.valueOf(registrationId.toUpperCase()));
        authProvider.setProviderId(oAuth2UserInfo.getId());
        authProvider.setEmail(oAuth2UserInfo.getEmail());
        authProvider.setName(oAuth2UserInfo.getName());
        authProvider.setProfileImage(oAuth2UserInfo.getImageUrl());

        user.getAuthProviders().add(authProvider);
        userRepository.save(user);

        log.info("Auth provider created for user: {} with provider: {}", user.getEmail(), registrationId);
    }

    private void createOrUpdateAuthProvider(User user, String registrationId, OAuth2UserInfo oAuth2UserInfo) {
        AuthProvider.ProviderType providerType = AuthProvider.ProviderType.valueOf(registrationId.toUpperCase());

        Optional<AuthProvider> existingProvider = user.getAuthProviders().stream()
                .filter(provider -> provider.getProvider() == providerType)
                .findFirst();

        if (existingProvider.isPresent()) {
            AuthProvider authProvider = existingProvider.get();
            authProvider.setEmail(oAuth2UserInfo.getEmail());
            authProvider.setName(oAuth2UserInfo.getName());
            authProvider.setProfileImage(oAuth2UserInfo.getImageUrl());
            log.info("Auth provider updated for user: {}", user.getEmail());
        } else {
            createAuthProvider(user, registrationId, oAuth2UserInfo);
        }
    }

//    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
//        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
//        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());
//        
//        log.info("Processing OAuth2 user from provider: {} with email: {}", registrationId, oAuth2UserInfo.getEmail());
//
//        if (!StringUtils.hasText(oAuth2UserInfo.getEmail())) {
//            throw new OAuth2AuthenticationProcessingException("Email not found from OAuth2 provider");
//        }
//
//        Optional<User> userOptional = userRepository.findByEmail(oAuth2UserInfo.getEmail());
//        User user;
//        
//        if (userOptional.isPresent()) {
//            user = userOptional.get();
//            log.info("Existing user found: {}", user.getEmail());
//            
//            // Check if this user was registered via OAuth2 with this provider
//            if (!hasAuthProvider(user, registrationId)) {
//                // User exists but doesn't have this OAuth2 provider linked
//                user = linkOAuth2Provider(user, oAuth2UserRequest, oAuth2UserInfo);
//            } else {
//                // User exists and has this provider, update info
//                user = updateExistingUser(user, oAuth2UserInfo);
//            }
//        } else {
//            log.info("Creating new user from OAuth2 provider: {}", oAuth2UserInfo.getEmail());
//            user = registerNewUser(oAuth2UserRequest, oAuth2UserInfo);
//        }
//
//        // Update last login
//        user.setLastLoginAt(LocalDateTime.now());
//        user = userRepository.save(user);
//
//        return new CustomOAuth2User(user, oAuth2User.getAttributes());
//    }


    private User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo) {
        // Only update if the information is not already present or is newer
        if (!StringUtils.hasText(existingUser.getFirstName()) || 
            !existingUser.getFirstName().equals(oAuth2UserInfo.getFirstName())) {
            existingUser.setFirstName(oAuth2UserInfo.getFirstName());
        }
        
        if (!StringUtils.hasText(existingUser.getLastName()) || 
            !existingUser.getLastName().equals(oAuth2UserInfo.getLastName())) {
            existingUser.setLastName(oAuth2UserInfo.getLastName());
        }
        
        if (!StringUtils.hasText(existingUser.getProfileImage()) || 
            !existingUser.getProfileImage().equals(oAuth2UserInfo.getImageUrl())) {
            existingUser.setProfileImage(oAuth2UserInfo.getImageUrl());
        }
        
        existingUser.setEmailVerified(true);

        User updatedUser = userRepository.save(existingUser);
        log.info("User updated: {}", updatedUser.getEmail());

        return updatedUser;
    }

    private User linkOAuth2Provider(User existingUser, OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo) {
        log.info("Linking OAuth2 provider {} to existing user: {}", 
                oAuth2UserRequest.getClientRegistration().getRegistrationId(), existingUser.getEmail());
        
        // Update user info from OAuth2 if available
        existingUser = updateExistingUser(existingUser, oAuth2UserInfo);
        
        // Create auth provider link
        createAuthProvider(existingUser, oAuth2UserRequest.getClientRegistration().getRegistrationId(), oAuth2UserInfo);
        
        return existingUser;
    }


    private boolean hasAuthProvider(User user, String registrationId) {
        AuthProvider.ProviderType providerType = AuthProvider.ProviderType.valueOf(registrationId.toUpperCase());
        return user.getAuthProviders().stream()
                .anyMatch(provider -> provider.getProvider() == providerType);
    }
}
