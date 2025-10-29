package org.example.business;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.business.dto.AccountDeletionEvent;
import org.example.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountDeletionPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishAccountDeletion(String userKeycloakId, String reason) {
        try {
            AccountDeletionEvent event = new AccountDeletionEvent(
                    userKeycloakId,
                    LocalDateTime.now(),
                    reason
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ACCOUNT_DELETION_EXCHANGE,
                    RabbitMQConfig.ACCOUNT_DELETION_ROUTING_KEY,
                    event
            );

            log.info("Published account deletion event for user: {}", userKeycloakId);
        } catch (Exception e) {
            log.error("Failed to publish account deletion event for user: {}", userKeycloakId, e);
            throw new RuntimeException("Failed to publish account deletion event", e);
        }
    }
}
