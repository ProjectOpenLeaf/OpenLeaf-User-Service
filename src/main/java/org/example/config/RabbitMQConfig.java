package org.example.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration for Account Deletion
 * This configuration should be included in all services that participate in account deletion
 */
@Configuration
public class RabbitMQConfig {

    // Exchange name
    public static final String ACCOUNT_DELETION_EXCHANGE = "account.deletion.exchange";

    // Queue names
    public static final String ASSIGNMENT_DELETION_QUEUE = "assignment.deletion.queue";
    public static final String SCHEDULING_DELETION_QUEUE = "scheduling.deletion.queue";
    public static final String JOURNAL_DELETION_QUEUE = "journal.deletion.queue";

    // Routing key
    public static final String ACCOUNT_DELETION_ROUTING_KEY = "account.deleted";

    /**
     * Declare the topic exchange for account deletion events
     */
    @Bean
    public TopicExchange accountDeletionExchange() {
        return new TopicExchange(ACCOUNT_DELETION_EXCHANGE);
    }

    /**
     * Queue for Assignment Service to process deletions
     */
    @Bean
    public Queue assignmentDeletionQueue() {
        return new Queue(ASSIGNMENT_DELETION_QUEUE, true); // durable queue
    }

    /**
     * Queue for Scheduling Service to process deletions
     */
    @Bean
    public Queue schedulingDeletionQueue() {
        return new Queue(SCHEDULING_DELETION_QUEUE, true); // durable queue
    }

    /**
     * Queue for Journal Service to process deletions
     */
    @Bean
    public Queue journalDeletionQueue() {
        return new Queue(JOURNAL_DELETION_QUEUE, true); // durable queue
    }

    /**
     * Bind Assignment queue to the exchange with routing key
     */
    @Bean
    public Binding assignmentBinding(Queue assignmentDeletionQueue, TopicExchange accountDeletionExchange) {
        return BindingBuilder
                .bind(assignmentDeletionQueue)
                .to(accountDeletionExchange)
                .with(ACCOUNT_DELETION_ROUTING_KEY);
    }

    /**
     * Bind Scheduling queue to the exchange with routing key
     */
    @Bean
    public Binding schedulingBinding(Queue schedulingDeletionQueue, TopicExchange accountDeletionExchange) {
        return BindingBuilder
                .bind(schedulingDeletionQueue)
                .to(accountDeletionExchange)
                .with(ACCOUNT_DELETION_ROUTING_KEY);
    }

    /**
     * Bind Journal queue to the exchange with routing key
     */
    @Bean
    public Binding journalBinding(Queue journalDeletionQueue, TopicExchange accountDeletionExchange) {
        return BindingBuilder
                .bind(journalDeletionQueue)
                .to(accountDeletionExchange)
                .with(ACCOUNT_DELETION_ROUTING_KEY);
    }

    /**
     * JSON message converter for serializing/deserializing messages
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate configured with JSON converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}







