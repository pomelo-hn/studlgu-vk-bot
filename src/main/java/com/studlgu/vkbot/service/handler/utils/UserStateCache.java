package com.studlgu.vkbot.service.handler.utils;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * In-memory кэш для хранения состояний пользователей.
 * Автоматически очищает просроченные записи.
 */
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

	/**
	 * Устанавливает состояние ожидания фото для пользователя.
	 */
	public void setWaitingPhoto(Long userId) {
		setWaitingPhoto(userId, DEFAULT_TTL);
	}

	/**
	 * Устанавливает состояние ожидания фото с кастомным TTL.
	 */
	public void setWaitingPhoto(Long userId, Duration ttl) {
		cache.put(userId, new CacheEntry(Instant.now().plusMillis(ttl.toMillis())));
	}

	/**
	 * Проверяет, ожидает ли пользователь отправки фото.
	 */
	public boolean isWaitingPhoto(Long userId) {
		CacheEntry entry = cache.get(userId);
		if (entry == null) {
			return false;
		}

		if (entry.isExpired()) {
			cache.remove(userId);
			return false;
		}

		return true;
	}

	/**
	 * Удаляет состояние ожидания фото для пользователя.
	 */
	public void clearWaitingPhoto(Long userId) {
		cache.remove(userId);
	}

	/**
	 * Очищает все просроченные записи.
	 */
	private void cleanupExpiredEntries() {
		cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
	}

	/**
	 * Останавливает планировщик при уничтожении бина.
	 */
	@PreDestroy
	public void shutdown() {
		if (cleanupScheduler != null && !cleanupScheduler.isShutdown()) {
			cleanupScheduler.shutdown();
		}
	}

	/**
	 * Запись в кэше с временем истечения.
	 */
	private record CacheEntry(Instant expireAt) {
		boolean isExpired() {
			return Instant.now().isAfter(expireAt);
		}
	}
}
