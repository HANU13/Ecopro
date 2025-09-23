package com.litemax.ECoPro.service.user;

import com.litemax.ECoPro.dto.auth.AddressRequest;
import com.litemax.ECoPro.dto.auth.AddressResponse;
import com.litemax.ECoPro.entity.auth.User;
import com.litemax.ECoPro.entity.auth.Address;
import com.litemax.ECoPro.exception.ResourceNotFoundException;
import com.litemax.ECoPro.exception.ValidationException;
import com.litemax.ECoPro.repository.auth.UserRepository;
import com.litemax.ECoPro.repository.auth.AddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public List<AddressResponse> getUserAddresses(String email) {
        log.debug("Fetching addresses for user: {}", email);
        
        User user = getUserByEmail(email);
        List<Address> addresses = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId());
        
        log.debug("Found {} addresses for user: {}", addresses.size(), email);
        return addresses.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public AddressResponse getAddressById(String email, Long addressId) {
        log.debug("Fetching address {} for user: {}", addressId, email);
        
        User user = getUserByEmail(email);
        Address address = getAddressByIdAndUser(addressId, user.getId());
        
        return convertToResponse(address);
    }

    public AddressResponse addAddress(String email, AddressRequest request) {
        log.info("Adding new address for user: {} - Type: {}, City: {}", 
                email, request.getType(), request.getCity());
        
        User user = getUserByEmail(email);
        
        // Validate address limit (optional business rule)
        validateAddressLimit(user.getId());
        
        Address address = buildAddressFromRequest(request, user);
        
        // Handle default address logic
        handleDefaultAddressLogic(address, user.getId());
        
        address = addressRepository.save(address);
        log.info("Address added successfully with ID: {} for user: {}", address.getId(), email);

        return convertToResponse(address);
    }

    public AddressResponse updateAddress(String email, Long addressId, AddressRequest request) {
        log.info("Updating address {} for user: {} - Type: {}, City: {}", 
                addressId, email, request.getType(), request.getCity());
        
        User user = getUserByEmail(email);
        Address address = getAddressByIdAndUser(addressId, user.getId());

        // Update address fields
        updateAddressFields(address, request);
        
        // Handle default address logic if needed
        if (request.isDefault() && !address.isDefault()) {
            setOtherAddressesNonDefault(user.getId());
            address.setDefault(true);
        }

        address = addressRepository.save(address);
        log.info("Address {} updated successfully for user: {}", addressId, email);

        return convertToResponse(address);
    }

    public void deleteAddress(String email, Long addressId) {
        log.info("Deleting address {} for user: {}", addressId, email);
        
        User user = getUserByEmail(email);
        Address address = getAddressByIdAndUser(addressId, user.getId());

        // Prevent deletion if it's the only address and business rule requires at least one
        // validateMinimumAddresses(user.getId());

        boolean wasDefault = address.isDefault();
        addressRepository.delete(address);

        // If deleted address was default, set another address as default
        if (wasDefault) {
            setNewDefaultAddress(user.getId());
        }

        log.info("Address {} deleted successfully for user: {}", addressId, email);
    }

    public void setDefaultAddress(String email, Long addressId) {
        log.info("Setting address {} as default for user: {}", addressId, email);
        
        User user = getUserByEmail(email);
        Address address = getAddressByIdAndUser(addressId, user.getId());

        if (address.isDefault()) {
            log.warn("Address {} is already default for user: {}", addressId, email);
            throw new ValidationException("Address is already set as default");
        }

        // Remove default from other addresses
        setOtherAddressesNonDefault(user.getId());

        // Set this address as default
        address.setDefault(true);
        addressRepository.save(address);

        log.info("Address {} set as default successfully for user: {}", addressId, email);
    }

    // Helper Methods

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private Address getAddressByIdAndUser(Long addressId, Long userId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with ID: " + addressId));
    }

    private void validateAddressLimit(Long userId) {
        long addressCount = addressRepository.countByUserId(userId);
        final int MAX_ADDRESSES = 10; // Business rule: max 10 addresses per user
        
        if (addressCount >= MAX_ADDRESSES) {
            log.warn("User {} attempting to exceed address limit. Current count: {}", userId, addressCount);
            throw new ValidationException("Maximum number of addresses (" + MAX_ADDRESSES + ") reached");
        }
    }

    private Address buildAddressFromRequest(AddressRequest request, User user) {
        Address address = new Address();
        address.setUser(user);
        updateAddressFields(address, request);
        return address;
    }

    private void updateAddressFields(Address address, AddressRequest request) {
        address.setFullName(request.getFullName());
        address.setPhone(request.getPhone());
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setZipCode(request.getZipCode());
        address.setCountry(request.getCountry());
        address.setType(request.getType() != null ? request.getType() : Address.AddressType.OTHER);
    }

    private void handleDefaultAddressLogic(Address address, Long userId) {
        long userAddressCount = addressRepository.countByUserId(userId);
        
        // If this is the first address or marked as default, set as default
        if (userAddressCount == 0 || address.isDefault()) {
            if (address.isDefault()) {
                setOtherAddressesNonDefault(userId);
            }
            address.setDefault(true);
        }
    }

    private void setOtherAddressesNonDefault(Long userId) {
        log.debug("Setting all addresses as non-default for user: {}", userId);
        addressRepository.updateDefaultToFalseForUser(userId);
    }

    private void setNewDefaultAddress(Long userId) {
        List<Address> remainingAddresses = addressRepository.findByUserIdOrderByCreatedAtAsc(userId);
        if (!remainingAddresses.isEmpty()) {
            Address newDefault = remainingAddresses.get(0);
            newDefault.setDefault(true);
            addressRepository.save(newDefault);
            log.info("Set address {} as new default for user: {}", newDefault.getId(), userId);
        } else {
            log.debug("No remaining addresses for user: {}", userId);
        }
    }

    private void validateMinimumAddresses(Long userId) {
        long addressCount = addressRepository.countByUserId(userId);
        if (addressCount <= 1) {
            throw new ValidationException("Cannot delete the last address. At least one address is required.");
        }
    }

    private AddressResponse convertToResponse(Address address) {
        return AddressResponse.builder()
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
}