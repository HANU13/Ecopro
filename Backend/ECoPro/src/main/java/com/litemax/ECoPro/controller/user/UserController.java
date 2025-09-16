package com.litemax.ECoPro.controller.user;

import com.litemax.ECoPro.dto.user.UserDTO;
import com.litemax.ECoPro.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getLoggedInUser() {
        return ResponseEntity.ok(userService.getLoggedInUserProfile());
    }
}