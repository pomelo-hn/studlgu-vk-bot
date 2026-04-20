package com.studlgu.vkbot.service.handler.utils;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class UserStateCache {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);
    private static final Duration CLEANUP_INTERVAL = Duration.ofMinutes(5);

    private final Map<Long, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupScheduler;

    public UserStateCache() {
        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "user-state-cache-cleanup");
            thread.setDaemon(true);
            return thread;
        });
        cleanupScheduler.scheduleAtFixedRate(
                this::cleanupExpiredEntries,
                CLEANUP_INTERVAL.toMinutes(),
                CLEANUP_INTERVAL.toMinutes(),
                TimeUnit.MINUTES
        );
    }

    public void setState(Long userId, UserState state) {
        setState(userId, state, DEFAULT_TTL);
    }

    public void setState(Long userId, UserState state, Duration ttl) {
        cache.put(userId, new CacheEntry(state, Instant.now().plusMillis(ttl.toMillis())));
    }

    public Optional<UserState> getState(Long userId) {
        CacheEntry entry = cache.get(userId);
        if (entry == null) return Optional.empty();
        if (entry.isExpired()) {
            cache.remove(userId);
            return Optional.empty();
        }
        return Optional.of(entry.state());
    }

    public void clearState(Long userId) {
        cache.remove(userId);
    }

    private void cleanupExpiredEntries() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    @PreDestroy
    public void shutdown() {
        if (cleanupScheduler != null && !cleanupScheduler.isShutdown()) {
            cleanupScheduler.shutdown();
        }
    }

    private record CacheEntry(UserState state, Instant expireAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expireAt);
        }
    }
}
