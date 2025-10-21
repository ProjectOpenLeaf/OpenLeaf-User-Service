package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.business.UserRegister;
import org.example.business.dto.RegisterRequest;
import org.example.business.dto.RegisterResponse;
import org.example.domain.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserRegister userRegister;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {

        User user = userRegister.register(
                request.getKeycloakId(),
                request.getUsername(),
                request.getEmail(),
                request.getFirstName(),
                request.getLastName()
        );

        RegisterResponse response = RegisterResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .createdAt(user.getCreatedAt())
                .message("User registered successfully")
                .build();

        return ResponseEntity.ok(response);
    }
}
