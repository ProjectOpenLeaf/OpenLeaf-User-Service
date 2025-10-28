package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.business.GetAllTherapists;
import org.example.business.UserRegister;
import org.example.business.dto.RegisterRequest;
import org.example.business.dto.RegisterResponse;
import org.example.business.dto.UserListResponse;
import org.example.domain.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserRegister userRegister;
    private final GetAllTherapists getAllTherapists;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {

        User user = userRegister.register(
                request.getKeycloakId(),
                request.getUsername(),
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                request.getRoles() // <-- Pass roles from request
        );

        RegisterResponse response = RegisterResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .createdAt(user.getCreatedAt())
                .roles(user.getRoles())
                .message("User registered successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/therapists")
    public ResponseEntity<List<UserListResponse>> getAllUsers() {
        // Returns all users - frontend should filter by role if needed
        List<User> users = getAllTherapists.getAllTherapists();

        List<UserListResponse> responses = users.stream()
                .map(user -> UserListResponse.builder()
                        .id(user.getId())
                        .keycloakId(user.getKeycloakId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .createdAt(user.getCreatedAt())
                        .roles(user.getRoles())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
}