package com.studlgu.vkbot.service.handler.utils;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
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
}