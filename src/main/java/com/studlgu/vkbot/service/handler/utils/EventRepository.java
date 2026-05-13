package com.studlgu.vkbot.service.handler.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.studlgu.vkbot.model.Event;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class EventRepository {

    private final String storagePath;
    private final ObjectMapper objectMapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private File storageFile;

    public EventRepository(@Value("${vkbot.events.storage.path}") String storagePath) {
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

    public List<Event> findAll() {
        lock.readLock().lock();
        try {
            return objectMapper.readValue(storageFile, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to read events", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void save(Event event) {
        lock.writeLock().lock();
        try {
            if (event.getId() == null) {
                event.setId(UUID.randomUUID().toString());
            }
            List<Event> events = new ArrayList<>(findAllUnsafe());
            events.add(event);
            objectMapper.writeValue(storageFile, events);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save event", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean deleteByIdPrefix(String prefix) {
        lock.writeLock().lock();
        try {
            List<Event> events = new ArrayList<>(findAllUnsafe());
            boolean removed = events.removeIf(e -> e.getId() != null && e.getId().startsWith(prefix));
            if (removed) {
                objectMapper.writeValue(storageFile, events);
            }
            return removed;
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete event by prefix", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private List<Event> findAllUnsafe() throws IOException {
        return objectMapper.readValue(storageFile, new TypeReference<>() {});
    }
}
