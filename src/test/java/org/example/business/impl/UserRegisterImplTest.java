package org.example.business.impl;

import org.example.domain.User;
import org.example.persistance.UserRepository;
import org.example.persistance.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserRegisterImplTest {
    private UserRepository userRepository;
    private UserRegisterImpl userRegister;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userRegister = new UserRegisterImpl(userRepository);
    }

    @Test
    void register_newUser_savesAndReturnsUser() {
        String keycloakId = "abc-123";
        String username = "john_doe";
        String email = "john@example.com";
        String firstName = "John";
        String lastName = "Doe";
        Set<String> roles = Set.of("USER");

        // Mock repository: user does not exist yet
        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.empty());

        // Mock save to just return the same entity with an ID
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        when(userRepository.save(captor.capture())).thenAnswer(invocation -> {
            UserEntity entity = invocation.getArgument(0);
            entity.setId(1L); // simulate DB-generated ID
            return entity;
        });

        // Call method
        User result = userRegister.register(keycloakId, username, email, firstName, lastName, roles);

        // Verify repository calls
        verify(userRepository).findByKeycloakId(keycloakId);
        verify(userRepository).save(any(UserEntity.class));

        // Check captured entity
        UserEntity savedEntity = captor.getValue();
        assertEquals(keycloakId, savedEntity.getKeycloakId());
        assertEquals(username, savedEntity.getUsername());
        assertEquals(email, savedEntity.getEmail());
        assertEquals(firstName, savedEntity.getFirstName());
        assertEquals(lastName, savedEntity.getLastName());
        assertEquals(roles, savedEntity.getRoles());
        assertNotNull(savedEntity.getCreatedAt());

        // Check returned User
        assertEquals(1L, result.getId());
        assertEquals(keycloakId, result.getKeycloakId());
        assertEquals(username, result.getUsername());
        assertEquals(email, result.getEmail());
        assertEquals(firstName, result.getFirstName());
        assertEquals(lastName, result.getLastName());
        assertEquals(roles, result.getRoles());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    void register_existingUser_updatesRoles() {
        String keycloakId = "abc-123";
        Set<String> roles = Set.of("ADMIN");

        UserEntity existing = UserEntity.builder()
                .id(1L)
                .keycloakId(keycloakId)
                .username("existing_user")
                .email("existing@example.com")
                .createdAt(LocalDateTime.now().minusDays(1))
                .roles(Set.of("USER"))
                .build();

        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userRegister.register(keycloakId, existing.getUsername(),
                existing.getEmail(), "Existing", "User", roles);

        // Roles should be updated
        assertEquals(roles, result.getRoles());

        // Verify repository calls
        verify(userRepository).findByKeycloakId(keycloakId);
        verify(userRepository).save(existing);
    }
}