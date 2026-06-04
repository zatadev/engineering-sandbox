package com.zatadev.notificationservice.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String KEY_PREFIX = "processed_event:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    // Atomically marks the event as seen; returns true if it was already processed.
    public boolean isDuplicate(UUID eventId) {
        String key = KEY_PREFIX + eventId;
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(key, "1", TTL);
        if (Boolean.FALSE.equals(isNew)) {
            log.warn("Duplicate event detected, skipping: eventId={}", eventId);
            return true;
        }
        return false;
    }
}