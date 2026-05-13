package com.studlgu.vkbot.service.handler.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.studlgu.vkbot.model.Appeal;
import com.studlgu.vkbot.model.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DataBackupService {

    private static final int VERSION = 1;

    private final EventRepository eventRepository;
    private final AppealRepository appealRepository;
    private final MotivationRepository motivationRepository;
    private final PhotoStorage photoStorage;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public String exportAsBase64() {
        try {
            DataBackup backup = new DataBackup(
                    VERSION,
                    Instant.now(),
                    eventRepository.findAll(),
                    appealRepository.findAll(),
                    motivationRepository.findAll(),
                    photoStorage.getAllStoredPhotos().stream()
                            .map(photo -> new BackupPhoto(
                                    photo.fileName(),
                                    Base64.getEncoder().encodeToString(photo.data())))
                            .toList()
            );
            byte[] json = objectMapper.writeValueAsBytes(backup);
            return Base64.getEncoder().encodeToString(json);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export data backup", e);
        }
    }

    public void importFromBase64(String encodedBackup) {
        if (encodedBackup == null || encodedBackup.isBlank()) {
            throw new IllegalArgumentException("Backup text is required");
        }

        try {
            String normalizedBackup = normalizeBackupText(encodedBackup);
            byte[] json = Base64.getDecoder().decode(normalizedBackup);
            DataBackup backup = objectMapper.readValue(json, DataBackup.class);
            if (backup.version() != VERSION) {
                throw new IllegalArgumentException("Unsupported backup version: " + backup.version());
            }
            List<PhotoStorage.StoredPhoto> menuPhotos = emptyIfNull(backup.menuPhotos()).stream()
                    .map(photo -> new PhotoStorage.StoredPhoto(
                            photo.fileName(),
                            Base64.getDecoder().decode(photo.dataBase64())))
                    .toList();

            eventRepository.saveAll(emptyIfNull(backup.events()));
            appealRepository.saveAll(emptyIfNull(backup.appeals()));
            motivationRepository.saveAll(emptyIfNull(backup.motivations()));
            photoStorage.saveNamedPhotos(menuPhotos);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException("Failed to import data backup", e);
        }
    }

    private String normalizeBackupText(String encodedBackup) {
        return encodedBackup.lines()
                .filter(line -> !line.trim().matches("(?i)^backup:\\d+/\\d+$"))
                .reduce("", (left, right) -> left + right)
                .replaceAll("\\s+", "");
    }

    private <T> List<T> emptyIfNull(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record DataBackup(
            int version,
            Instant exportedAt,
            List<Event> events,
            List<Appeal> appeals,
            List<String> motivations,
            List<BackupPhoto> menuPhotos
    ) {}

    private record BackupPhoto(String fileName, String dataBase64) {}
}
