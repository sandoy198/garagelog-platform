package com.garagelog.core.service;

import com.garagelog.core.dto.CreateServiceRecordResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String IDEMPOTENCY_PREFIX = "idempotency:service-record:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;

    public CreateServiceRecordResponse executeIdempotent(
            String idempotencyKey,
            Supplier<CreateServiceRecordResponse> action) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return action.get();
        }

        String redisKey = IDEMPOTENCY_PREFIX + idempotencyKey.trim();
        Object cached = redisTemplate.opsForValue().get(redisKey);

        if (cached instanceof CreateServiceRecordResponse cachedResponse) {
            log.info("Idempotency HIT for key={}, returning cached response without re-processing DB/Kafka", redisKey);
            return cachedResponse;
        }

        CreateServiceRecordResponse response = action.get();
        redisTemplate.opsForValue().set(redisKey, response, IDEMPOTENCY_TTL);
        log.info("Idempotency STORED for key={} with TTL 24h", redisKey);

        return response;
    }
}
