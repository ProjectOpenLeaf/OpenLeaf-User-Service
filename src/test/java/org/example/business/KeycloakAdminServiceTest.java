package org.example.business;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakAdminServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private KeycloakAdminService keycloakAdminService;

    @Test
    void deleteUserFromKeycloak_shouldSucceed_whenValidRequest() {
        // Arrange
        String userKeycloakId = "user-123";
        setupKeycloakService();

        // Mock token response
        KeycloakAdminService.TokenResponse tokenResponse = new KeycloakAdminService.TokenResponse();
        tokenResponse.setAccess_token("mock-admin-token");

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class),
                eq(KeycloakAdminService.TokenResponse.class)))
                .thenReturn(ResponseEntity.ok(tokenResponse));

        // Mock delete response
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE),
                any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        // Act
        keycloakAdminService.deleteUserFromKeycloak(userKeycloakId);

        // Assert
        verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class),
                eq(KeycloakAdminService.TokenResponse.class));
        verify(restTemplate).exchange(contains(userKeycloakId), eq(HttpMethod.DELETE),
                any(HttpEntity.class), eq(Void.class));
    }

    @Test
    void deleteUserFromKeycloak_shouldThrowException_whenTokenFetchFails() {
        // Arrange
        String userKeycloakId = "user-123";
        setupKeycloakService();

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class),
                eq(KeycloakAdminService.TokenResponse.class)))
                .thenThrow(new RuntimeException("Token fetch failed"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                keycloakAdminService.deleteUserFromKeycloak(userKeycloakId)
        );

        assertTrue(exception.getMessage().contains("Failed to delete user from Keycloak"));
        verify(restTemplate, never()).exchange(anyString(), eq(HttpMethod.DELETE),
                any(HttpEntity.class), eq(Void.class));
    }

    @Test
    void deleteUserFromKeycloak_shouldThrowException_whenDeleteFails() {
        // Arrange
        String userKeycloakId = "user-123";
        setupKeycloakService();

        KeycloakAdminService.TokenResponse tokenResponse = new KeycloakAdminService.TokenResponse();
        tokenResponse.setAccess_token("mock-token");

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class),
                eq(KeycloakAdminService.TokenResponse.class)))
                .thenReturn(ResponseEntity.ok(tokenResponse));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE),
                any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                keycloakAdminService.deleteUserFromKeycloak(userKeycloakId)
        );

        assertTrue(exception.getMessage().contains("Failed to delete user from Keycloak"));
    }

    @Test
    void deleteUserFromKeycloak_shouldUseCorrectUrl() {
        // Arrange
        String userKeycloakId = "user-abc-123";
        String serverUrl = "http://keycloak:8080";
        String realm = "openleaf-realm";

        ReflectionTestUtils.setField(keycloakAdminService, "keycloakServerUrl", serverUrl);
        ReflectionTestUtils.setField(keycloakAdminService, "realm", realm);
        ReflectionTestUtils.setField(keycloakAdminService, "adminClientId", "admin-client");
        ReflectionTestUtils.setField(keycloakAdminService, "adminClientSecret", "secret");

        KeycloakAdminService.TokenResponse tokenResponse = new KeycloakAdminService.TokenResponse();
        tokenResponse.setAccess_token("token");

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class),
                eq(KeycloakAdminService.TokenResponse.class)))
                .thenReturn(ResponseEntity.ok(tokenResponse));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE),
                any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        // Act
        keycloakAdminService.deleteUserFromKeycloak(userKeycloakId);

        // Assert
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(
                urlCaptor.capture(),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(Void.class)
        );

        String capturedUrl = urlCaptor.getValue();
        assertTrue(capturedUrl.contains(serverUrl));
        assertTrue(capturedUrl.contains(realm));
        assertTrue(capturedUrl.contains(userKeycloakId));
        assertTrue(capturedUrl.contains("/admin/realms/"));
        assertTrue(capturedUrl.contains("/users/"));
    }

    @Test
    void deleteUserFromKeycloak_shouldUseBearerToken() {
        // Arrange
        String userKeycloakId = "user-123";
        setupKeycloakService();

        String expectedToken = "test-bearer-token";
        KeycloakAdminService.TokenResponse tokenResponse = new KeycloakAdminService.TokenResponse();
        tokenResponse.setAccess_token(expectedToken);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class),
                eq(KeycloakAdminService.TokenResponse.class)))
                .thenReturn(ResponseEntity.ok(tokenResponse));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE),
                any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        // Act
        keycloakAdminService.deleteUserFromKeycloak(userKeycloakId);

        // Assert
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                anyString(),
                eq(HttpMethod.DELETE),
                entityCaptor.capture(),
                eq(Void.class)
        );

        HttpHeaders headers = entityCaptor.getValue().getHeaders();
        assertNotNull(headers.get("Authorization"));
    }

    private void setupKeycloakService() {
        ReflectionTestUtils.setField(keycloakAdminService, "keycloakServerUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(keycloakAdminService, "realm", "test-realm");
        ReflectionTestUtils.setField(keycloakAdminService, "adminClientId", "admin-client");
        ReflectionTestUtils.setField(keycloakAdminService, "adminClientSecret", "secret");
    }
}