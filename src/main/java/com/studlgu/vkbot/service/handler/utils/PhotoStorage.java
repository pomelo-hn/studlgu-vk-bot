package com.studlgu.vkbot.service.handler.utils;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class PhotoStorage {

	@Value("${vkbot.photo.storage.path}")
	private String storagePath;

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private File storageDir;

	@PostConstruct
	public void init() {
		storageDir = new File(storagePath);
		if (!storageDir.exists()) {
			storageDir.mkdirs();
		}
	}

	/**
	 * Сохраняет список фотографий, удаляя предыдущие
	 */
	public void savePhotos(List<byte[]> photos) throws IOException {
		lock.writeLock().lock();
		try {
			// Удаляем старые файлы
			deleteAllFiles();

			// Сохраняем новые
			int index = 0;
			for (byte[] photo : photos) {
				File file = new File(storageDir, "photo_" + index++ + ".jpg");
				try (FileOutputStream fos = new FileOutputStream(file)) {
					fos.write(photo);
				}
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Возвращает список файлов фотографий
	 */
	public List<File> getAllPhotos() {
		lock.readLock().lock();
		try {
			File[] files = storageDir.listFiles();
			List<File> result = new ArrayList<>();

			if (files != null) {
				for (File file : files) {
					if (file.isFile()) {
						result.add(file);
					}
				}
			}
			return result;
		} finally {
			lock.readLock().unlock();
		}
	}

	public List<StoredPhoto> getAllStoredPhotos() throws IOException {
		lock.readLock().lock();
		try {
			File[] files = storageDir.listFiles();
			List<StoredPhoto> result = new ArrayList<>();

			if (files != null) {
				for (File file : files) {
					if (file.isFile()) {
						result.add(new StoredPhoto(file.getName(), Files.readAllBytes(file.toPath())));
					}
				}
			}
			result.sort(Comparator.comparing(StoredPhoto::fileName));
			return result;
		} finally {
			lock.readLock().unlock();
		}
	}

	public void saveNamedPhotos(List<StoredPhoto> photos) throws IOException {
		lock.writeLock().lock();
		try {
			deleteAllFiles();

			for (StoredPhoto photo : photos) {
				File file = new File(storageDir, safeFileName(photo.fileName()));
				try (FileOutputStream fos = new FileOutputStream(file)) {
					fos.write(photo.data());
				}
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Удаляет все файлы из директории
	 */
	private void deleteAllFiles() throws IOException {
		File[] files = storageDir.listFiles();
		if (files != null) {
			for (File file : files) {
				Files.deleteIfExists(file.toPath());
			}
		}
	}

	private String safeFileName(String fileName) {
		if (fileName == null || fileName.isBlank()) {
			throw new IllegalArgumentException("Photo file name is required");
		}
		String normalized = new File(fileName).getName();
		if (!normalized.equals(fileName) || normalized.contains("..")) {
			throw new IllegalArgumentException("Unsafe photo file name: " + fileName);
		}
		return normalized;
	}

	public record StoredPhoto(String fileName, byte[] data) {}
}
