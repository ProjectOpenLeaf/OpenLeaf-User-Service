package org.example.business;

import org.example.business.dto.AccountDeletionEvent;
import org.example.config.RabbitMQConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountDeletionPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private AccountDeletionPublisher publisher;

    @Test
    void publishAccountDeletion_shouldPublishEventToRabbitMQ() {
        // Arrange
        String userKeycloakId = "user-123";
        String reason = "User requested deletion";

        // Act
        publisher.publishAccountDeletion(userKeycloakId, reason);

        // Assert
        ArgumentCaptor<AccountDeletionEvent> eventCaptor = ArgumentCaptor.forClass(AccountDeletionEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.ACCOUNT_DELETION_EXCHANGE),
                eq(RabbitMQConfig.ACCOUNT_DELETION_ROUTING_KEY),
                eventCaptor.capture()
        );

        AccountDeletionEvent event = eventCaptor.getValue();
        assertEquals(userKeycloakId, event.getUserKeycloakId());
        assertEquals(reason, event.getReason());
//            assertNotNull(event.getTimestamp());
    }

//        @Test
//        void publishAccountDeletion_shouldSetCurrentTimestamp() {
//            // Arrange
//            String userKeycloakId = "user-123";
//            String reason = "Test";
//            LocalDateTime before = LocalDateTime.now().minusSeconds(1);
//
//            // Act
//            publisher.publishAccountDeletion(userKeycloakId, reason);
//
//            LocalDateTime after = LocalDateTime.now().plusSeconds(1);
//
//            // Assert
//            ArgumentCaptor<AccountDeletionEvent> eventCaptor = ArgumentCaptor.forClass(AccountDeletionEvent.class);
//            verify(rabbitTemplate).convertAndSend(
//                    anyString(), anyString(), eventCaptor.capture()
//            );
//
//            LocalDateTime timestamp = eventCaptor.getValue().getTimestamp();
//            assertTrue(timestamp.isAfter(before));
//            assertTrue(timestamp.isBefore(after));
//        }

    @Test
    void publishAccountDeletion_shouldThrowException_whenRabbitMQFails() {
        // Arrange
        String userKeycloakId = "user-123";
        String reason = "Test";

        doThrow(new RuntimeException("RabbitMQ connection error"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(AccountDeletionEvent.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                publisher.publishAccountDeletion(userKeycloakId, reason)
        );

        assertTrue(exception.getMessage().contains("Failed to publish account deletion event"));
    }

    @Test
    void publishAccountDeletion_shouldUseCorrectExchangeAndRoutingKey() {
        // Arrange
        String userKeycloakId = "user-456";
        String reason = "GDPR compliance";

        // Act
        publisher.publishAccountDeletion(userKeycloakId, reason);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq("account.deletion.exchange"),
                eq("account.deleted"),
                any(AccountDeletionEvent.class)
        );
    }
}