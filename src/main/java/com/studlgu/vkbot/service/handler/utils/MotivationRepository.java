package com.studlgu.vkbot.service.handler.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class MotivationRepository {

    private final String storagePath;
    private final ObjectMapper objectMapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private File storageFile;

    public MotivationRepository(@Value("${vkbot.motivations.storage.path:./motivations.json}") String storagePath) {
        this.storagePath = storagePath;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() throws IOException {
        storageFile = new File(storagePath);
        if (!storageFile.exists()) {
            File parent = storageFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("Failed to create directories: " + parent);
            }
            objectMapper.writeValue(storageFile, new ArrayList<>());
        }
    }

    public List<String> findAll() {
        lock.readLock().lock();
        try {
            return objectMapper.readValue(storageFile, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to read motivations", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void add(String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        lock.writeLock().lock();
        try {
            List<String> motivations = new ArrayList<>(findAllUnsafe());
            motivations.add(text.trim());
            objectMapper.writeValue(storageFile, motivations);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save motivation", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private List<String> findAllUnsafe() throws IOException {
        return objectMapper.readValue(storageFile, new TypeReference<>() {});
    }
}
