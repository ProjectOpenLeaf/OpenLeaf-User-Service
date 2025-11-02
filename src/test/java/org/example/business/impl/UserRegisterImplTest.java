package org.example.business.impl;

import org.example.domain.User;
import org.example.persistance.UserRepository;
import org.example.persistance.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegisterImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserRegisterImpl userRegister;

    @Test
    void register_shouldCreateNewUser_whenNotExists() {
        // Arrange
        String keycloakId = "keycloak-123";
        String username = "john_doe";
        String email = "john@example.com";
        String firstName = "John";
        String lastName = "Doe";
        Set<String> roles = Set.of("client_user");

        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.empty());

        UserEntity savedEntity = UserEntity.builder()
                .id(1L)
                .keycloakId(keycloakId)
                .username(username)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .roles(roles)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.save(any(UserEntity.class))).thenReturn(savedEntity);

        // Act
        User result = userRegister.register(keycloakId, username, email, firstName, lastName, roles);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(keycloakId, result.getKeycloakId());
        assertEquals(username, result.getUsername());
        assertEquals(email, result.getEmail());
        assertEquals(firstName, result.getFirstName());
        assertEquals(lastName, result.getLastName());
        assertEquals(roles, result.getRoles());
        assertNotNull(result.getCreatedAt());

        verify(userRepository).findByKeycloakId(keycloakId);
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void register_shouldUpdateRoles_whenUserExists() {
        // Arrange
        String keycloakId = "keycloak-123";
        Set<String> oldRoles = Set.of("client_user");
        Set<String> newRoles = Set.of("client_user", "client_therapist");

        UserEntity existingUser = UserEntity.builder()
                .id(1L)
                .keycloakId(keycloakId)
                .username("john_doe")
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .roles(oldRoles)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userRegister.register(keycloakId, "john_doe", "john@example.com",
                "John", "Doe", newRoles);

        // Assert
        assertEquals(newRoles, result.getRoles());

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertEquals(newRoles, captor.getValue().getRoles());
    }

    @Test
    void register_shouldSetCreatedAt_forNewUser() {
        // Arrange
        String keycloakId = "keycloak-123";
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.empty());

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        when(userRepository.save(captor.capture())).thenAnswer(invocation -> {
            UserEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        // Act
        userRegister.register(keycloakId, "user", "user@example.com", "First", "Last", Set.of("client_user"));

        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        // Assert
        UserEntity savedEntity = captor.getValue();
        assertNotNull(savedEntity.getCreatedAt());
        assertTrue(savedEntity.getCreatedAt().isAfter(before));
        assertTrue(savedEntity.getCreatedAt().isBefore(after));
    }

    @Test
    void register_shouldMapAllFieldsCorrectly() {
        // Arrange
        String keycloakId = "keycloak-xyz";
        String username = "jane_smith";
        String email = "jane@example.com";
        String firstName = "Jane";
        String lastName = "Smith";
        Set<String> roles = Set.of("client_therapist", "client_user");

        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.empty());

        UserEntity savedEntity = UserEntity.builder()
                .id(99L)
                .keycloakId(keycloakId)
                .username(username)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .roles(roles)
                .createdAt(LocalDateTime.of(2024, 1, 15, 10, 30))
                .build();

        when(userRepository.save(any(UserEntity.class))).thenReturn(savedEntity);

        // Act
        User result = userRegister.register(keycloakId, username, email, firstName, lastName, roles);

        // Assert
        assertEquals(99L, result.getId());
        assertEquals(keycloakId, result.getKeycloakId());
        assertEquals(username, result.getUsername());
        assertEquals(email, result.getEmail());
        assertEquals(firstName, result.getFirstName());
        assertEquals(lastName, result.getLastName());
        assertEquals(2, result.getRoles().size());
        assertTrue(result.getRoles().contains("client_therapist"));
        assertTrue(result.getRoles().contains("client_user"));
    }

    @Test
    void register_shouldHandleEmptyRoles() {
        // Arrange
        String keycloakId = "keycloak-123";
        Set<String> emptyRoles = Set.of();

        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.empty());

        UserEntity savedEntity = UserEntity.builder()
                .id(1L)
                .keycloakId(keycloakId)
                .username("user")
                .email("user@example.com")
                .firstName("First")
                .lastName("Last")
                .roles(emptyRoles)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.save(any(UserEntity.class))).thenReturn(savedEntity);

        // Act
        User result = userRegister.register(keycloakId, "user", "user@example.com",
                "First", "Last", emptyRoles);

        // Assert
        assertNotNull(result.getRoles());
        assertTrue(result.getRoles().isEmpty());
    }

    @Test
    void register_shouldPreserveExistingUserData_whenUpdatingRoles() {
        // Arrange
        String keycloakId = "keycloak-123";
        String originalUsername = "original_user";
        String originalEmail = "original@example.com";
        LocalDateTime originalCreatedAt = LocalDateTime.now().minusDays(30);

        UserEntity existingUser = UserEntity.builder()
                .id(1L)
                .keycloakId(keycloakId)
                .username(originalUsername)
                .email(originalEmail)
                .firstName("Original")
                .lastName("User")
                .roles(Set.of("client_user"))
                .createdAt(originalCreatedAt)
                .build();

        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Set<String> newRoles = Set.of("client_therapist");

        // Act
        User result = userRegister.register(keycloakId, originalUsername, originalEmail,
                "Original", "User", newRoles);

        // Assert
        assertEquals(1L, result.getId());
        assertEquals(keycloakId, result.getKeycloakId());
        assertEquals(originalUsername, result.getUsername());
        assertEquals(originalEmail, result.getEmail());
        assertEquals(originalCreatedAt, result.getCreatedAt());
        assertEquals(newRoles, result.getRoles());
    }
}