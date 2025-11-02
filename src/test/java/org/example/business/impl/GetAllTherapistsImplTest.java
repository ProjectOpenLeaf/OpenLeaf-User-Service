package org.example.business.impl;

import static org.junit.jupiter.api.Assertions.*;

import org.example.domain.User;
import org.example.persistance.UserRepository;
import org.example.persistance.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetAllTherapistsImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetAllTherapistsImpl getAllTherapists;

    @Test
    void getAllTherapists_shouldReturnAllTherapists() {
        // Arrange
        UserEntity therapist1 = createTherapistEntity(1L, "therapist1", "therapist1@example.com");
        UserEntity therapist2 = createTherapistEntity(2L, "therapist2", "therapist2@example.com");

        when(userRepository.findAllByRoles("client_therapist"))
                .thenReturn(Arrays.asList(therapist1, therapist2));

        // Act
        List<User> result = getAllTherapists.getAllTherapists();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals(1L, result.get(0).getId());
        assertEquals("therapist1", result.get(0).getUsername());

        assertEquals(2L, result.get(1).getId());
        assertEquals("therapist2", result.get(1).getUsername());

        verify(userRepository).findAllByRoles("client_therapist");
    }

    @Test
    void getAllTherapists_shouldReturnEmptyList_whenNoTherapists() {
        // Arrange
        when(userRepository.findAllByRoles("client_therapist"))
                .thenReturn(Collections.emptyList());

        // Act
        List<User> result = getAllTherapists.getAllTherapists();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository).findAllByRoles("client_therapist");
    }

    @Test
    void getAllTherapists_shouldMapAllFields() {
        // Arrange
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 15, 10, 30);
        UserEntity therapist = UserEntity.builder()
                .id(10L)
                .keycloakId("keycloak-xyz")
                .username("dr_smith")
                .email("smith@example.com")
                .firstName("John")
                .lastName("Smith")
                .roles(Set.of("client_therapist"))
                .createdAt(createdAt)
                .build();

        when(userRepository.findAllByRoles("client_therapist"))
                .thenReturn(Collections.singletonList(therapist));

        // Act
        List<User> result = getAllTherapists.getAllTherapists();

        // Assert
        assertEquals(1, result.size());
        User user = result.get(0);

        assertEquals(10L, user.getId());
        assertEquals("keycloak-xyz", user.getKeycloakId());
        assertEquals("dr_smith", user.getUsername());
        assertEquals("smith@example.com", user.getEmail());
        assertEquals("John", user.getFirstName());
        assertEquals("Smith", user.getLastName());
        assertEquals(Set.of("client_therapist"), user.getRoles());
        assertEquals(createdAt, user.getCreatedAt());
    }

    @Test
    void getAllTherapists_shouldOnlyCallRepositoryOnce() {
        // Arrange
        when(userRepository.findAllByRoles("client_therapist"))
                .thenReturn(Collections.emptyList());

        // Act
        getAllTherapists.getAllTherapists();

        // Assert
        verify(userRepository, times(1)).findAllByRoles("client_therapist");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void getAllTherapists_shouldHandleMultipleRoles() {
        // Arrange
        UserEntity therapist = UserEntity.builder()
                .id(1L)
                .keycloakId("keycloak-123")
                .username("therapist")
                .email("therapist@example.com")
                .firstName("Jane")
                .lastName("Doe")
                .roles(Set.of("client_therapist", "client_user", "admin"))
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findAllByRoles("client_therapist"))
                .thenReturn(Collections.singletonList(therapist));

        // Act
        List<User> result = getAllTherapists.getAllTherapists();

        // Assert
        assertEquals(1, result.size());
        assertEquals(3, result.get(0).getRoles().size());
        assertTrue(result.get(0).getRoles().contains("client_therapist"));
        assertTrue(result.get(0).getRoles().contains("client_user"));
        assertTrue(result.get(0).getRoles().contains("admin"));
    }

    private UserEntity createTherapistEntity(Long id, String username, String email) {
        return UserEntity.builder()
                .id(id)
                .keycloakId("keycloak-" + id)
                .username(username)
                .email(email)
                .firstName("First" + id)
                .lastName("Last" + id)
                .roles(Set.of("client_therapist"))
                .createdAt(LocalDateTime.now())
                .build();
    }
}






