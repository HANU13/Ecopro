package com.litemax.ECoPro.controller.user;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.litemax.ECoPro.dto.auth.AddressRequest;
import com.litemax.ECoPro.dto.auth.AddressResponse;
import com.litemax.ECoPro.service.user.AddressService;

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
@RequestMapping("/api/users/addresses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Address Management", description = "User address management APIs")
@SecurityRequirement(name = "bearerAuth")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @Operation(
        summary = "Get user addresses",
        description = "Retrieves all addresses for the authenticated user, ordered by default status and creation date"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Addresses retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<List<AddressResponse>> getUserAddresses(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Fetching addresses for user: {}", userDetails.getUsername());
        List<AddressResponse> addresses = addressService.getUserAddresses(userDetails.getUsername());
        log.debug("Found {} addresses for user: {}", addresses.size(), userDetails.getUsername());
        return ResponseEntity.ok(addresses);
    }

    @PostMapping
    @Operation(
        summary = "Add new address",
        description = "Creates a new address for the authenticated user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Address created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<AddressResponse> addAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Address details to create", required = true)
            @Valid @RequestBody AddressRequest request) {
        
        log.info("Adding new address for user: {} - Type: {}, City: {}", 
                userDetails.getUsername(), request.getType(), request.getCity());
        
        AddressResponse response = addressService.addAddress(userDetails.getUsername(), request);
        
        log.info("Address created successfully with ID: {} for user: {}", 
                response.getId(), userDetails.getUsername());
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{addressId}")
    @Operation(
        summary = "Update address",
        description = "Updates an existing address for the authenticated user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Address updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Address not found", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<AddressResponse> updateAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Address ID to update", required = true)
            @PathVariable Long addressId,
            @Parameter(description = "Updated address details", required = true)
            @Valid @RequestBody AddressRequest request) {
        
        log.info("Updating address {} for user: {} - Type: {}, City: {}", 
                addressId, userDetails.getUsername(), request.getType(), request.getCity());
        
        AddressResponse response = addressService.updateAddress(userDetails.getUsername(), addressId, request);
        
        log.info("Address {} updated successfully for user: {}", addressId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{addressId}")
    @Operation(
        summary = "Delete address",
        description = "Deletes an existing address for the authenticated user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Address deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Address not found", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<Void> deleteAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Address ID to delete", required = true)
            @PathVariable Long addressId) {
        
        log.info("Deleting address {} for user: {}", addressId, userDetails.getUsername());
        addressService.deleteAddress(userDetails.getUsername(), addressId);
        log.info("Address {} deleted successfully for user: {}", addressId, userDetails.getUsername());
        
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{addressId}/default")
    @Operation(
        summary = "Set address as default",
        description = "Sets the specified address as the default address for the authenticated user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Default address set successfully"),
        @ApiResponse(responseCode = "400", description = "Address is already default", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Address not found", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<Void> setDefaultAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Address ID to set as default", required = true)
            @PathVariable Long addressId) {
        
        log.info("Setting address {} as default for user: {}", addressId, userDetails.getUsername());
        addressService.setDefaultAddress(userDetails.getUsername(), addressId);
        log.info("Address {} set as default successfully for user: {}", addressId, userDetails.getUsername());
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{addressId}")
    @Operation(
        summary = "Get address by ID",
        description = "Retrieves a specific address by ID for the authenticated user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Address retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Address not found", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<AddressResponse> getAddressById(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Address ID to retrieve", required = true)
            @PathVariable Long addressId) {
        
        log.debug("Fetching address {} for user: {}", addressId, userDetails.getUsername());
        AddressResponse address = addressService.getAddressById(userDetails.getUsername(), addressId);
        return ResponseEntity.ok(address);
    }
}