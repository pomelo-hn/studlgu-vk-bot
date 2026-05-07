package com.studlgu.vkbot.service.handler.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.studlgu.vkbot.model.Appeal;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class AppealRepository {

    private final String storagePath;
    private final ObjectMapper objectMapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private File storageFile;

    public AppealRepository(@Value("${vkbot.appeals.storage.path}") String storagePath) {
        this.storagePath = storagePath;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
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

    public List<Appeal> findAll() {
        lock.readLock().lock();
        try {
            return objectMapper.readValue(storageFile, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to read appeals", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void save(Appeal appeal) {
        lock.writeLock().lock();
        try {
            if (appeal.getId() == null) {
                appeal.setId(UUID.randomUUID().toString());
            }
            List<Appeal> appeals = new ArrayList<>(findAllUnsafe());
            appeals.add(appeal);
            objectMapper.writeValue(storageFile, appeals);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save appeal", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void saveAll(List<Appeal> appeals) {
        lock.writeLock().lock();
        try {
            objectMapper.writeValue(storageFile, appeals);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save appeals", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private List<Appeal> findAllUnsafe() throws IOException {
        return objectMapper.readValue(storageFile, new TypeReference<>() {});
    }
}
