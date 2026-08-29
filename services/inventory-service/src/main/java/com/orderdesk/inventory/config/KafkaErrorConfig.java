package com.orderdesk.inventory.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Retry policy: 3 attempts with exponential backoff (500ms, 1s, 2s) then publish to <topic>.DLT
 * with the exception details in headers. Poison messages (bad JSON, missing headers) skip retries.
 * Business rejections (insufficient stock) are not failures: the listener catches them and commits.
 */
@Configuration
public class KafkaErrorConfig {
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
        var recoverer = new DeadLetterPublishingRecoverer(template);
        var backOff = new ExponentialBackOff(500L, 2.0);
        backOff.setMaxAttempts(3);
        var handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(DeserializationException.class,
                IllegalArgumentException.class,
                com.fasterxml.jackson.core.JsonProcessingException.class);
        return handler;
    }
}
