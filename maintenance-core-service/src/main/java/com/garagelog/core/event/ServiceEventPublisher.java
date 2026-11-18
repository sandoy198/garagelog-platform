package com.garagelog.core.event;

import com.garagelog.core.config.KafkaTopicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceEventPublisher {

    private final KafkaTemplate<String, ServiceLoggedEvent> kafkaTemplate;

    public void publishServiceLogged(ServiceLoggedEvent event) {
        String key = event.getVehicleId().toString();
        kafkaTemplate.send(KafkaTopicConfig.SERVICE_LOGGED_TOPIC, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Kafka event sent successfully: topic={}, key={}, partition={}, offset={}, eventId={}",
                                KafkaTopicConfig.SERVICE_LOGGED_TOPIC,
                                key,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                event.getEventId());
                    } else {
                        log.error("Failed to send Kafka event: topic={}, key={}, eventId={}, error={}",
                                KafkaTopicConfig.SERVICE_LOGGED_TOPIC,
                                key,
                                event.getEventId(),
                                ex.getMessage(),
                                ex);
                    }
                });
    }
}
