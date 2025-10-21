package org.example.domain;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String keycloakId;
    private String username;
    @Nullable
    private String email;
    private String firstName;
    private String lastName;
    private LocalDateTime createdAt;
}
