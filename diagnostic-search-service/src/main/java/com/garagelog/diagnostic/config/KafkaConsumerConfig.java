package com.garagelog.diagnostic.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public CommonErrorHandler kafkaErrorHandler() {
        // Retry 2 times with a 1-second fixed backoff before giving up on a failed message
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (consumerRecord, exception) -> log.error(
                        "Kafka message permanently failed after retries: topic={}, partition={}, offset={}, key={}, error={}",
                        consumerRecord.topic(),
                        consumerRecord.partition(),
                        consumerRecord.offset(),
                        consumerRecord.key(),
                        exception.getMessage(),
                        exception
                ),
                new FixedBackOff(1000L, 2L)
        );
        return errorHandler;
    }
}
