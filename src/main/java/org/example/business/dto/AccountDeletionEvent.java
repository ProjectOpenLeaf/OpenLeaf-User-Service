package org.example.business.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountDeletionEvent {
    private String userKeycloakId;
    private LocalDateTime deletionTimestamp;
    private String reason;
}
