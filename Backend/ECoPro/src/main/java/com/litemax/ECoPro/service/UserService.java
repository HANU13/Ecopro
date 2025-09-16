package com.litemax.ECoPro.service;


import com.litemax.ECoPro.dto.user.UserDTO;
import com.litemax.ECoPro.entity.auth.User;
import com.litemax.ECoPro.repository.auth.UserRepository;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserDTO getLoggedInUserProfile() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<User> userOptional = userRepository.findByEmail(userDetails.getUsername());

        if (!userOptional.isPresent()) {
            throw new ResourceNotFoundException("User not found with email: " + userDetails.getUsername());
        }

        User user = userOptional.get();
        return UserDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhone())
                .roles(user.getRoles())
                .build();
    }
}