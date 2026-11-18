package com.garagelog.core.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String SERVICE_LOGGED_TOPIC = "garagelog.service.logged";

    @Bean
    public NewTopic serviceLoggedTopic() {
        return TopicBuilder.name(SERVICE_LOGGED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
