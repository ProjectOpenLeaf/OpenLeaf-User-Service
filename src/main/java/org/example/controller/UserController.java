package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.business.AccountDeletionPublisher;
import org.example.business.GetAllTherapists;
import org.example.business.KeycloakAdminService;
import org.example.business.UserRegister;
import org.example.business.dto.RegisterRequest;
import org.example.business.dto.RegisterResponse;
import org.example.business.dto.UserListResponse;
import org.example.domain.User;
import org.example.persistance.UserRepository;
import org.example.persistance.entity.UserEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserRegister userRegister;
    private final GetAllTherapists getAllTherapists;
    private final UserRepository userRepository;
    private final AccountDeletionPublisher accountDeletionPublisher;
    private final KeycloakAdminService keycloakAdminService;

    @DeleteMapping("/{keycloakId}")
    public ResponseEntity<String> deleteUser(
            @PathVariable String keycloakId,
            @RequestParam(required = false, defaultValue = "User requested") String reason) {

        try {
            log.info("Processing deletion request for user: {} - Reason: {}", keycloakId, reason);

            // 1. Find the user
            UserEntity user = userRepository.findByKeycloakId(keycloakId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 2. Publish deletion event to RabbitMQ first
            // This ensures other services (Assignment, Scheduling, Journal) can clean up their data
            log.info("Publishing account deletion event to RabbitMQ for user: {}", keycloakId);
            accountDeletionPublisher.publishAccountDeletion(keycloakId, reason);
            log.info("Account deletion event published successfully");

            // 3. Delete the user from Keycloak
            log.info("Deleting user from Keycloak: {}", keycloakId);
            keycloakAdminService.deleteUserFromKeycloak(keycloakId);
            log.info("User deleted from Keycloak successfully");

            // 4. Delete the user from User Profile Service database
            log.info("Deleting user from User Profile Service database: {}", keycloakId);
            userRepository.delete(user);
            log.info("User deleted from User Profile Service successfully");

            log.info("Account deletion completed successfully for user: {}", keycloakId);
            return ResponseEntity.ok("User account deleted successfully from all systems");

        } catch (Exception e) {
            log.error("Failed to delete user: {} - Error: {}", keycloakId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Failed to delete user account: " + e.getMessage());
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("User Service is running");
    }

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