package org.example.business;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminService {

    private final RestTemplate restTemplate;

    @Value("${keycloak.auth-server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin.client-id}")
    private String adminClientId;

    @Value("${keycloak.admin.client-secret}")
    private String adminClientSecret;

    public void deleteUserFromKeycloak(String userKeycloakId) {
        try {
            log.info("Attempting to delete user from Keycloak: {}", userKeycloakId);

            // Get admin access token
            String adminToken = getAdminToken();

            // Delete user from Keycloak
            String deleteUrl = String.format("%s/admin/realms/%s/users/%s",
                    keycloakServerUrl, realm, userKeycloakId);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                    deleteUrl,
                    HttpMethod.DELETE,
                    request,
                    Void.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully deleted user from Keycloak: {}", userKeycloakId);
            } else {
                log.error("Failed to delete user from Keycloak. Status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to delete user from Keycloak");
            }

        } catch (Exception e) {
            log.error("Error deleting user from Keycloak: {}", userKeycloakId, e);
            throw new RuntimeException("Failed to delete user from Keycloak: " + e.getMessage());
        }
    }

    /**
     * Get admin access token from Keycloak
     * Uses client credentials grant
     */
    public String getAdminToken() {
        try {
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token",
                    keycloakServerUrl, realm);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/x-www-form-urlencoded");

            String body = String.format(
                    "grant_type=client_credentials&client_id=%s&client_secret=%s",
                    adminClientId, adminClientSecret
            );

            HttpEntity<String> request = new HttpEntity<>(body, headers);

            ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                    tokenUrl,
                    request,
                    TokenResponse.class
            );

            if (response.getBody() != null && response.getBody().getAccessToken() != null) {
                return response.getBody().getAccessToken();
            } else {
                throw new RuntimeException("Failed to obtain admin token from Keycloak");
            }

        } catch (Exception e) {
            log.error("Error obtaining admin token from Keycloak", e);
            throw new RuntimeException("Failed to authenticate with Keycloak: " + e.getMessage());
        }
    }

    /**
     * Inner class to deserialize Keycloak token response
     */
    public static class TokenResponse {
        private String access_token;

        public String getAccessToken() {
            return access_token;
        }

        public void setAccess_token(String access_token) {
            this.access_token = access_token;
        }
    }
}







