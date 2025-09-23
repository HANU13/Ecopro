package com.litemax.ECoPro.controller.auth;

import com.litemax.ECoPro.dto.auth.OAuth2LoginResponse;
import com.litemax.ECoPro.service.auth.OAuth2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/oauth2")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "OAuth2 Authentication", description = "OAuth2 social login APIs")
public class OAuth2Controller {

    private final OAuth2Service oAuth2Service;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @GetMapping("/authorization/{provider}")
    @Operation(
        summary = "Get OAuth2 authorization URL",
        description = "Returns the authorization URL for the specified OAuth2 provider"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authorization URL generated successfully"),
        @ApiResponse(responseCode = "400", description = "Unsupported provider")
    })
    public ResponseEntity<Map<String, String>> getAuthorizationUrl(
            @Parameter(description = "OAuth2 provider (google, facebook, github)", required = true)
            @PathVariable String provider,
            @Parameter(description = "Redirect URI after authentication")
            @RequestParam(required = false) String redirectUri,
            HttpServletRequest request) {
        
        log.info("OAuth2 authorization URL requested for provider: {}", provider);
        
        String authUrl = oAuth2Service.generateAuthorizationUrl(provider, redirectUri, request);
        
        Map<String, String> response = Map.of(
            "authorizationUrl", authUrl,
            "provider", provider,
            "state", "generated"
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/login/{provider}")
    @Operation(
        summary = "Initiate OAuth2 login",
        description = "Redirects to OAuth2 provider for authentication"
    )
    public void initiateOAuth2Login(
            @Parameter(description = "OAuth2 provider", required = true)
            @PathVariable String provider,
            @RequestParam(required = false) String redirectUri,
            HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        
        log.info("Initiating OAuth2 login for provider: {}", provider);
        
        String authUrl = oAuth2Service.generateAuthorizationUrl(provider, redirectUri, request);
        response.sendRedirect(authUrl);
    }

    @PostMapping("/unlink/{provider}")
    @Operation(
        summary = "Unlink OAuth2 provider",
        description = "Removes the OAuth2 provider from user's account"
    )
    public ResponseEntity<Map<String, String>> unlinkProvider(
            @Parameter(description = "OAuth2 provider to unlink", required = true)
            @PathVariable String provider,
            @Parameter(description = "User ID", required = true)
            @RequestParam Long userId) {
        
        log.info("Unlinking OAuth2 provider {} for user: {}", provider, userId);
        
        oAuth2Service.unlinkProvider(userId, provider);
        
        Map<String, String> response = Map.of(
            "message", "Provider unlinked successfully",
            "provider", provider
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/providers/{userId}")
    @Operation(
        summary = "Get linked OAuth2 providers",
        description = "Returns all OAuth2 providers linked to user's account"
    )
    public ResponseEntity<OAuth2LoginResponse> getLinkedProviders(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId) {
        
        log.debug("Getting linked providers for user: {}", userId);
        
        OAuth2LoginResponse response = oAuth2Service.getLinkedProviders(userId);
        return ResponseEntity.ok(response);
    }
}