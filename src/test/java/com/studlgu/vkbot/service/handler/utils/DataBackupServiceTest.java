package com.studlgu.vkbot.service.handler.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.studlgu.vkbot.model.Appeal;
import com.studlgu.vkbot.model.AppealStatus;
import com.studlgu.vkbot.model.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DataBackupServiceTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void exportAsBase64IncludesJsonDataAndPhotoFiles() throws Exception {
        TestContext ctx = createContext(tempDir.resolve("source"));
        Event event = new Event("event-1", "Meeting", LocalDate.of(2026, 5, 14),
                LocalTime.of(10, 30), "Discuss plans", "Room 1");
        Appeal appeal = new Appeal("appeal-1", 42L, "Need help", List.of("https://vk/photo.jpg"),
                List.of("photo-10_20_key"), AppealStatus.OPEN, Instant.parse("2026-05-14T08:00:00Z"),
                null, null, null);

        ctx.eventRepository.save(event);
        ctx.appealRepository.save(appeal);
        ctx.motivationRepository.add("Keep going");
        ctx.photoStorage.saveNamedPhotos(List.of(new PhotoStorage.StoredPhoto("menu.jpg", new byte[] {1, 2, 3})));

        String encodedBackup = ctx.dataBackupService.exportAsBase64();

        JsonNode backup = objectMapper.readTree(Base64.getDecoder().decode(encodedBackup));
        assertThat(backup.get("version").asInt()).isEqualTo(1);
        assertThat(backup.get("events").get(0).get("title").asText()).isEqualTo("Meeting");
        assertThat(backup.get("appeals").get(0).get("text").asText()).isEqualTo("Need help");
        assertThat(backup.get("motivations").get(0).asText()).isEqualTo("Keep going");
        assertThat(backup.get("menuPhotos").get(0).get("fileName").asText()).isEqualTo("menu.jpg");
        assertThat(backup.get("menuPhotos").get(0).get("dataBase64").asText())
                .isEqualTo(Base64.getEncoder().encodeToString(new byte[] {1, 2, 3}));
    }

    @Test
    void importFromBase64ReplacesStoredData() throws Exception {
        TestContext source = createContext(tempDir.resolve("source"));
        source.eventRepository.save(new Event("event-1", "Meeting", LocalDate.of(2026, 5, 14),
                LocalTime.of(10, 30), "Discuss plans", "Room 1"));
        source.appealRepository.save(new Appeal("appeal-1", 42L, "Need help", List.of("https://vk/photo.jpg"),
                List.of("photo-10_20_key"), AppealStatus.OPEN, Instant.parse("2026-05-14T08:00:00Z"),
                null, null, null));
        source.motivationRepository.add("Keep going");
        source.photoStorage.saveNamedPhotos(List.of(new PhotoStorage.StoredPhoto("menu.jpg", new byte[] {1, 2, 3})));
        String encodedBackup = source.dataBackupService.exportAsBase64();

        TestContext target = createContext(tempDir.resolve("target"));
        target.eventRepository.save(new Event("old-event", "Old", LocalDate.of(2026, 1, 1),
                null, null, null));
        target.motivationRepository.add("Old motivation");
        target.photoStorage.saveNamedPhotos(List.of(new PhotoStorage.StoredPhoto("old.jpg", new byte[] {9})));

        target.dataBackupService.importFromBase64(encodedBackup);

        assertThat(target.eventRepository.findAll()).extracting(Event::getId).containsExactly("event-1");
        assertThat(target.appealRepository.findAll()).extracting(Appeal::getId).containsExactly("appeal-1");
        assertThat(target.motivationRepository.findAll()).containsExactly("Keep going");
        List<PhotoStorage.StoredPhoto> photos = target.photoStorage.getAllStoredPhotos();
        assertThat(photos).extracting(PhotoStorage.StoredPhoto::fileName).containsExactly("menu.jpg");
        assertThat(photos.getFirst().data()).containsExactly(1, 2, 3);
        assertThat(Files.exists(tempDir.resolve("target").resolve("photos").resolve("old.jpg"))).isFalse();
    }

    @Test
    void importFromBase64IgnoresExportPartHeaders() throws Exception {
        TestContext source = createContext(tempDir.resolve("source"));
        source.eventRepository.save(new Event("event-1", "Meeting", LocalDate.of(2026, 5, 14),
                LocalTime.of(10, 30), "Discuss plans", "Room 1"));
        String encodedBackup = source.dataBackupService.exportAsBase64();
        String pastedBackup = "backup:1/2\n"
                + encodedBackup.substring(0, encodedBackup.length() / 2)
                + "\nbackup:2/2\n"
                + encodedBackup.substring(encodedBackup.length() / 2);

        TestContext target = createContext(tempDir.resolve("target"));

        target.dataBackupService.importFromBase64(pastedBackup);

        assertThat(target.eventRepository.findAll()).extracting(Event::getId).containsExactly("event-1");
    }

    private TestContext createContext(Path root) throws Exception {
        Files.createDirectories(root);
        EventRepository eventRepository = new EventRepository(root.resolve("events.json").toString());
        eventRepository.init();
        AppealRepository appealRepository = new AppealRepository(root.resolve("appeals.json").toString());
        appealRepository.init();
        MotivationRepository motivationRepository = new MotivationRepository(root.resolve("motivations.json").toString());
        motivationRepository.init();
        PhotoStorage photoStorage = new PhotoStorage();
        ReflectionTestUtils.setField(photoStorage, "storagePath", root.resolve("photos").toString());
        photoStorage.init();
        return new TestContext(eventRepository, appealRepository, motivationRepository, photoStorage,
                new DataBackupService(eventRepository, appealRepository, motivationRepository, photoStorage));
    }

    private record TestContext(
            EventRepository eventRepository,
            AppealRepository appealRepository,
            MotivationRepository motivationRepository,
            PhotoStorage photoStorage,
            DataBackupService dataBackupService
    ) {}
}
