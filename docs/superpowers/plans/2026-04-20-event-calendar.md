# Event Calendar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Добавить в бот функцию просмотра событий (экзамены, дедлайны, мероприятия) в виде картинки-календаря; редакторы управляют событиями через VK-сообщения.

**Architecture:** EventRepository хранит события в JSON-файле с ReentrantReadWriteLock. EventService предоставляет фильтрацию и мутации. CalendarImageService генерирует PNG через Java AWT/Graphics2D. CommandHandler'ы вызывают сервисы и отправляют картинку в VK.

**Tech Stack:** Java 21, Spring Boot 4.0.3, VK Java SDK 1.0.16, Jackson (уже в проекте), Java AWT/Graphics2D (JDK built-in)

---

## Карта файлов

### Новые файлы
```
src/main/java/com/studlgu/vkbot/
  model/
    Event.java                              — POJO события (id, title, date, time, description, location)
  service/handler/utils/
    UserState.java                          — enum: AWAITING_PHOTO, AWAITING_ADD_EVENT, AWAITING_DELETE_ID
    EventRepository.java                    — чтение/запись events.json (Jackson + ReentrantReadWriteLock)
    EventService.java                       — фильтрация по неделе/месяцу, add, delete, listAll
    CalendarImageService.java               — генерация PNG (месяц и неделя)
    VkPhotoUploader.java                    — загрузка byte[] в VK (getMessagesUploadServer → upload → save)
  service/handler/command/impl/
    CalendarCommandHandler.java             — CALENDAR: отправляет подменю-клавиатуру
    CalendarWeekCommandHandler.java         — CALENDAR_WEEK: картинка ближайших 7 дней
    CalendarMonthCurrentCommandHandler.java — CALENDAR_MONTH_CURRENT: картинка текущего месяца
    CalendarMonthSelectCommandHandler.java  — CALENDAR_MONTH_SELECT: клавиатура выбора месяца
    CalendarMonthCommandHandler.java        — CALENDAR_MONTH: картинка месяца из payload.month
    AddEventCommandHandler.java             — ADD_EVENT: установить состояние AWAITING_ADD_EVENT
    DeleteEventCommandHandler.java          — DELETE_EVENT: показать список, установить AWAITING_DELETE_ID
  service/handler/callback/impl/
    AddEventInputHandler.java               — "add_event_input": парсинг и сохранение события
    DeleteEventInputHandler.java            — "delete_event_input": удаление события по ID

src/test/java/com/studlgu/vkbot/
  service/handler/utils/
    UserStateCacheTest.java
    EventRepositoryTest.java
    EventServiceTest.java
    CalendarImageServiceTest.java
```

### Изменённые файлы
```
src/main/java/com/studlgu/vkbot/
  model/Payload.java                        — добавить поле month (Integer)
  service/handler/utils/UserStateCache.java — хранить UserState вместо boolean
  service/handler/utils/StandardKeyboard.java — кнопка «Календарь событий» + 2 новых метода
  service/handler/command/CommandType.java  — новые значения
  service/handler/callback/CallbackType.java — новые значения
  service/handler/callback/CallbackService.java — инжектировать UserStateCache, проверять состояние
  service/handler/command/impl/UploadMenuCommandHandler.java — заменить setWaitingPhoto → setState
  service/handler/callback/impl/UploadPhotoHandler.java     — заменить isWaitingPhoto/clear → новый API
src/main/resources/application.properties  — добавить vkbot.events.storage.path
```

---

## Task 1: UserState enum + рефакторинг UserStateCache

**Files:**
- Create: `src/main/java/com/studlgu/vkbot/service/handler/utils/UserState.java`
- Modify: `src/main/java/com/studlgu/vkbot/service/handler/utils/UserStateCache.java`
- Modify: `src/main/java/com/studlgu/vkbot/service/handler/command/impl/UploadMenuCommandHandler.java`
- Modify: `src/main/java/com/studlgu/vkbot/service/handler/callback/impl/UploadPhotoHandler.java`
- Create: `src/test/java/com/studlgu/vkbot/service/handler/utils/UserStateCacheTest.java`

- [ ] **Step 1: Написать тест на UserStateCache с несколькими состояниями**

```java
package com.studlgu.vkbot.service.handler.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UserStateCacheTest {

    private UserStateCache cache;

    @BeforeEach
    void setUp() {
        cache = new UserStateCache();
    }

    @Test
    void setState_andGet_returnsState() {
        cache.setState(1L, UserState.AWAITING_ADD_EVENT);
        assertThat(cache.getState(1L)).isEqualTo(Optional.of(UserState.AWAITING_ADD_EVENT));
    }

    @Test
    void clearState_removesEntry() {
        cache.setState(1L, UserState.AWAITING_DELETE_ID);
        cache.clearState(1L);
        assertThat(cache.getState(1L)).isEmpty();
    }

    @Test
    void getState_unknownUser_returnsEmpty() {
        assertThat(cache.getState(999L)).isEmpty();
    }

    @Test
    void setState_overwritesPreviousState() {
        cache.setState(1L, UserState.AWAITING_PHOTO);
        cache.setState(1L, UserState.AWAITING_DELETE_ID);
        assertThat(cache.getState(1L)).isEqualTo(Optional.of(UserState.AWAITING_DELETE_ID));
    }
}
```

- [ ] **Step 2: Запустить тест — убедиться что падает**

```bash
./gradlew test --tests "com.studlgu.vkbot.service.handler.utils.UserStateCacheTest"
```

Ожидание: FAIL (классы не существуют)

- [ ] **Step 3: Создать UserState enum**

```java
package com.studlgu.vkbot.service.handler.utils;

public enum UserState {
    AWAITING_PHOTO,
    AWAITING_ADD_EVENT,
    AWAITING_DELETE_ID
}
```

- [ ] **Step 4: Переписать UserStateCache**

Заменить содержимое `UserStateCache.java` целиком:

```java
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
```

- [ ] **Step 5: Обновить UploadMenuCommandHandler** — заменить `userStateCache.setWaitingPhoto(userActor.getId())` на:

```java
userStateCache.setState(userActor.getId(), UserState.AWAITING_PHOTO);
```

Добавить импорт: `import com.studlgu.vkbot.service.handler.utils.UserState;`

- [ ] **Step 6: Обновить UploadPhotoHandler** — заменить проверку и очистку:

```java
// было:
boolean isServerWaitingPhoto = userStateCache.isWaitingPhoto(userActor.getId());
if (!isServerWaitingPhoto) { ... }
...
userStateCache.clearWaitingPhoto(userActor.getId());

// стало:
boolean isServerWaitingPhoto = userStateCache.getState(userActor.getId())
        .map(s -> s == UserState.AWAITING_PHOTO)
        .orElse(false);
if (!isServerWaitingPhoto) { ... }
...
userStateCache.clearState(userActor.getId());
```

Добавить импорт: `import com.studlgu.vkbot.service.handler.utils.UserState;`

- [ ] **Step 7: Запустить тесты — убедиться что проходят**

```bash
./gradlew test --tests "com.studlgu.vkbot.service.handler.utils.UserStateCacheTest"
```

Ожидание: PASS (4 теста)

- [ ] **Step 8: Запустить все тесты — проверить что ничего не сломалось**

```bash
./gradlew test
```

Ожидание: BUILD SUCCESS

- [ ] **Step 9: Коммит**

```bash
git add src/main/java/com/studlgu/vkbot/service/handler/utils/UserState.java \
        src/main/java/com/studlgu/vkbot/service/handler/utils/UserStateCache.java \
        src/main/java/com/studlgu/vkbot/service/handler/command/impl/UploadMenuCommandHandler.java \
        src/main/java/com/studlgu/vkbot/service/handler/callback/impl/UploadPhotoHandler.java \
        src/test/java/com/studlgu/vkbot/service/handler/utils/UserStateCacheTest.java
git commit -m "refactor: replace boolean UserStateCache with multi-state UserState enum"
```

---

## Task 2: Event model + EventRepository

**Files:**
- Create: `src/main/java/com/studlgu/vkbot/model/Event.java`
- Create: `src/main/java/com/studlgu/vkbot/service/handler/utils/EventRepository.java`
- Modify: `src/main/resources/application.properties`
- Create: `src/test/java/com/studlgu/vkbot/service/handler/utils/EventRepositoryTest.java`

- [ ] **Step 1: Написать тест на EventRepository**

```java
package com.studlgu.vkbot.service.handler.utils;

import com.studlgu.vkbot.model.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventRepositoryTest {

    @TempDir
    Path tempDir;

    private EventRepository repository;

    @BeforeEach
    void setUp() {
        repository = new EventRepository(tempDir.resolve("events.json").toString());
    }

    @Test
    void save_andFindAll_returnsEvent() {
        Event event = new Event(null, "Экзамен", LocalDate.of(2026, 5, 15),
                LocalTime.of(9, 0), "Билеты 1-40", "Корпус А");
        repository.save(event);

        List<Event> all = repository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.getFirst().getTitle()).isEqualTo("Экзамен");
        assertThat(all.getFirst().getId()).isNotNull();
    }

    @Test
    void deleteById_removesEvent() {
        Event event = new Event(null, "Митинг", LocalDate.of(2026, 6, 1),
                LocalTime.of(14, 0), "Краткое", "Онлайн");
        repository.save(event);
        String id = repository.findAll().getFirst().getId();

        boolean deleted = repository.deleteById(id);

        assertThat(deleted).isTrue();
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void deleteById_nonExistentId_returnsFalse() {
        assertThat(repository.deleteById("non-existent")).isFalse();
    }

    @Test
    void findAll_emptyFile_returnsEmptyList() {
        assertThat(repository.findAll()).isEmpty();
    }
}
```

- [ ] **Step 2: Запустить тест — убедиться что падает**

```bash
./gradlew test --tests "com.studlgu.vkbot.service.handler.utils.EventRepositoryTest"
```

Ожидание: FAIL

- [ ] **Step 3: Создать Event.java**

```java
package com.studlgu.vkbot.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    private String id;
    private String title;
    private LocalDate date;
    private LocalTime time;
    private String description;
    private String location;
}
```

- [ ] **Step 4: Создать EventRepository.java**

```java
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
            storageFile.getParentFile().mkdirs();
            objectMapper.writeValue(storageFile, new ArrayList<>());
        }
    }

    public List<Event> findAll() {
        lock.readLock().lock();
        try {
            return objectMapper.readValue(storageFile, new TypeReference<List<Event>>() {});
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

    public boolean deleteById(String id) {
        lock.writeLock().lock();
        try {
            List<Event> events = new ArrayList<>(findAllUnsafe());
            boolean removed = events.removeIf(e -> id.equals(e.getId()));
            if (removed) {
                objectMapper.writeValue(storageFile, events);
            }
            return removed;
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete event", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private List<Event> findAllUnsafe() throws IOException {
        return objectMapper.readValue(storageFile, new TypeReference<List<Event>>() {});
    }
}
```

> Примечание: конструктор принимает `String storagePath` напрямую, чтобы тесты могли создавать экземпляр без Spring-контекста, передавая путь к `@TempDir`. `@Value` на параметре конструктора работает в Spring-контексте так же, как на поле.

- [ ] **Step 5: Добавить в application.properties**

```properties
vkbot.events.storage.path=./events.json
```

- [ ] **Step 6: Запустить тесты EventRepository**

```bash
./gradlew test --tests "com.studlgu.vkbot.service.handler.utils.EventRepositoryTest"
```

Ожидание: PASS

- [ ] **Step 7: Коммит**

```bash
git add src/main/java/com/studlgu/vkbot/model/Event.java \
        src/main/java/com/studlgu/vkbot/service/handler/utils/EventRepository.java \
        src/main/resources/application.properties \
        src/test/java/com/studlgu/vkbot/service/handler/utils/EventRepositoryTest.java
git commit -m "feat: add Event model and EventRepository with JSON storage"
```

---

## Task 3: Payload.month + CommandType + CallbackType

**Files:**
- Modify: `src/main/java/com/studlgu/vkbot/model/Payload.java`
- Modify: `src/main/java/com/studlgu/vkbot/service/handler/command/CommandType.java`
- Modify: `src/main/java/com/studlgu/vkbot/service/handler/callback/CallbackType.java`

- [ ] **Step 1: Добавить поле month в Payload.java**

```java
@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Payload {
    private String command;
    private Integer month;  // для команды CALENDAR_MONTH: номер месяца (1-12)
}
```

- [ ] **Step 2: Добавить новые значения в CommandType.java**

```java
public enum CommandType {
    START,
    MOTIVATION,
    BELL_SCHEDULE,
    WHICH_WEEK,
    GET_MENU,
    UPLOAD_MENU,
    // Календарь событий
    CALENDAR,
    CALENDAR_WEEK,
    CALENDAR_MONTH_CURRENT,
    CALENDAR_MONTH_SELECT,
    CALENDAR_MONTH,
    // Управление событиями (редакторы)
    ADD_EVENT,
    DELETE_EVENT
}
```

- [ ] **Step 3: Добавить новые значения в CallbackType.java**

После строки `UPLOAD_PHOTO("upload_photo", "uploadPhotoHandler");` заменить `;` на `,` и добавить:

```java
    UPLOAD_PHOTO("upload_photo", "uploadPhotoHandler"),
    ADD_EVENT_INPUT("add_event_input", "addEventInputHandler"),
    DELETE_EVENT_INPUT("delete_event_input", "deleteEventInputHandler");
```

- [ ] **Step 4: Запустить все тесты**

```bash
./gradlew test
```

Ожидание: BUILD SUCCESS

- [ ] **Step 5: Коммит**

```bash
git add src/main/java/com/studlgu/vkbot/model/Payload.java \
        src/main/java/com/studlgu/vkbot/service/handler/command/CommandType.java \
        src/main/java/com/studlgu/vkbot/service/handler/callback/CallbackType.java
git commit -m "feat: add calendar command types and month field to Payload"
```

---

## Task 4: EventService

**Files:**
- Create: `src/main/java/com/studlgu/vkbot/service/handler/utils/EventService.java`
- Create: `src/test/java/com/studlgu/vkbot/service/handler/utils/EventServiceTest.java`

- [ ] **Step 1: Написать тесты на EventService**

```java
package com.studlgu.vkbot.service.handler.utils;

import com.studlgu.vkbot.model.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository repository;

    private EventService service;

    @BeforeEach
    void setUp() {
        service = new EventService(repository);
    }

    @Test
    void getEventsForWeek_returnsEventsWithinSevenDays() {
        LocalDate today = LocalDate.of(2026, 4, 20);
        Event inside = event("Inside", today.plusDays(3));
        Event outside = event("Outside", today.plusDays(8));
        when(repository.findAll()).thenReturn(List.of(inside, outside));

        List<Event> result = service.getEventsForWeek(today);

        assertThat(result).containsExactly(inside);
    }

    @Test
    void getEventsForMonth_returnsOnlyEventsInMonth() {
        YearMonth april = YearMonth.of(2026, 4);
        Event aprilEvent = event("April", LocalDate.of(2026, 4, 15));
        Event mayEvent = event("May", LocalDate.of(2026, 5, 1));
        when(repository.findAll()).thenReturn(List.of(aprilEvent, mayEvent));

        List<Event> result = service.getEventsForMonth(april);

        assertThat(result).containsExactly(aprilEvent);
    }

    @Test
    void addEvent_parsesRawInputAndSaves() {
        service.addEvent("Экзамен|2026-05-15|09:00|Билеты 1-40|Корпус А");
        verify(repository).save(argThat(e ->
                "Экзамен".equals(e.getTitle()) &&
                LocalDate.of(2026, 5, 15).equals(e.getDate()) &&
                LocalTime.of(9, 0).equals(e.getTime()) &&
                "Билеты 1-40".equals(e.getDescription()) &&
                "Корпус А".equals(e.getLocation())
        ));
    }

    @Test
    void addEvent_invalidFormat_throwsException() {
        assertThatThrownBy(() -> service.addEvent("только название"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("формат");
    }

    @Test
    void deleteEvent_delegatesToRepository() {
        when(repository.deleteById("abc")).thenReturn(true);
        assertThat(service.deleteEvent("abc")).isTrue();
    }

    @Test
    void listAll_returnsSortedByDate() {
        Event later = event("Later", LocalDate.of(2026, 6, 1));
        Event earlier = event("Earlier", LocalDate.of(2026, 4, 1));
        when(repository.findAll()).thenReturn(List.of(later, earlier));

        List<Event> result = service.listAll();

        assertThat(result).extracting(Event::getTitle)
                .containsExactly("Earlier", "Later");
    }

    private Event event(String title, LocalDate date) {
        return new Event("id-" + title, title, date, LocalTime.of(10, 0), "desc", "loc");
    }
}
```

- [ ] **Step 2: Запустить тест — убедиться что падает**

```bash
./gradlew test --tests "com.studlgu.vkbot.service.handler.utils.EventServiceTest"
```

Ожидание: FAIL

- [ ] **Step 3: Создать EventService.java**

```java
package com.studlgu.vkbot.service.handler.utils;

import com.studlgu.vkbot.model.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository repository;

    public List<Event> getEventsForWeek(LocalDate from) {
        LocalDate end = from.plusDays(7);
        return repository.findAll().stream()
                .filter(e -> !e.getDate().isBefore(from) && e.getDate().isBefore(end))
                .sorted(Comparator.comparing(Event::getDate).thenComparing(Event::getTime))
                .toList();
    }

    public List<Event> getEventsForMonth(YearMonth month) {
        return repository.findAll().stream()
                .filter(e -> YearMonth.from(e.getDate()).equals(month))
                .sorted(Comparator.comparing(Event::getDate).thenComparing(Event::getTime))
                .toList();
    }

    public void addEvent(String rawInput) {
        String[] parts = rawInput.split("\\|");
        if (parts.length != 5) {
            throw new IllegalArgumentException(
                    "Неверный формат. Ожидается: название|YYYY-MM-DD|HH:mm|описание|место");
        }
        try {
            Event event = new Event(
                    null,
                    parts[0].trim(),
                    LocalDate.parse(parts[1].trim()),
                    LocalTime.parse(parts[2].trim()),
                    parts[3].trim(),
                    parts[4].trim()
            );
            repository.save(event);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Неверный формат даты или времени. Дата: YYYY-MM-DD, Время: HH:mm");
        }
    }

    public boolean deleteEvent(String id) {
        return repository.deleteById(id);
    }

    public List<Event> listAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(Event::getDate).thenComparing(Event::getTime))
                .toList();
    }
}
```

- [ ] **Step 4: Запустить тесты EventService**

```bash
./gradlew test --tests "com.studlgu.vkbot.service.handler.utils.EventServiceTest"
```

Ожидание: PASS

- [ ] **Step 5: Коммит**

```bash
git add src/main/java/com/studlgu/vkbot/service/handler/utils/EventService.java \
        src/test/java/com/studlgu/vkbot/service/handler/utils/EventServiceTest.java
git commit -m "feat: add EventService with week/month filtering and CRUD"
```

---

## Task 5: CalendarImageService

**Files:**
- Create: `src/main/java/com/studlgu/vkbot/service/handler/utils/CalendarImageService.java`
- Create: `src/test/java/com/studlgu/vkbot/service/handler/utils/CalendarImageServiceTest.java`

- [ ] **Step 1: Добавить `java.awt.headless=true` в application.properties**

```properties
java.awt.headless=true
```

- [ ] **Step 2: Написать тесты на CalendarImageService**

```java
package com.studlgu.vkbot.service.handler.utils;

import com.studlgu.vkbot.model.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarImageServiceTest {

    private CalendarImageService service;

    @BeforeEach
    void setUp() {
        service = new CalendarImageService();
    }

    @Test
    void generateMonthImage_returnsNonEmptyPng() {
        List<Event> events = List.of(
                new Event("1", "Экзамен", LocalDate.of(2026, 4, 20),
                        LocalTime.of(9, 0), "desc", "loc")
        );
        byte[] png = service.generateMonthImage(YearMonth.of(2026, 4), events);
        assertThat(png).isNotEmpty();
        // PNG signature: первые 8 байт
        assertThat(png[0]).isEqualTo((byte) 0x89);
        assertThat(png[1]).isEqualTo((byte) 0x50); // 'P'
    }

    @Test
    void generateWeekImage_returnsNonEmptyPng() {
        List<Event> events = List.of(
                new Event("1", "Митинг", LocalDate.of(2026, 4, 21),
                        LocalTime.of(14, 0), "desc", "loc")
        );
        byte[] png = service.generateWeekImage(LocalDate.of(2026, 4, 20), events);
        assertThat(png).isNotEmpty();
        assertThat(png[0]).isEqualTo((byte) 0x89);
    }

    @Test
    void generateMonthImage_noEvents_doesNotThrow() {
        byte[] png = service.generateMonthImage(YearMonth.of(2026, 5), List.of());
        assertThat(png).isNotEmpty();
    }
}
```

- [ ] **Step 3: Запустить тест — убедиться что падает**

```bash
./gradlew test --tests "com.studlgu.vkbot.service.handler.utils.CalendarImageServiceTest"
```

Ожидание: FAIL

- [ ] **Step 4: Создать CalendarImageService.java**

```java
package com.studlgu.vkbot.service.handler.utils;

import com.studlgu.vkbot.model.Event;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CalendarImageService {

    private static final int MONTH_WIDTH = 840;
    private static final int MONTH_HEIGHT = 620;
    private static final int WEEK_WIDTH = 840;
    private static final int WEEK_HEIGHT = 280;

    private static final Color BG_COLOR = Color.WHITE;
    private static final Color GRID_COLOR = new Color(220, 220, 220);
    private static final Color HEADER_COLOR = new Color(50, 50, 50);
    private static final Color DAY_COLOR = new Color(60, 60, 60);
    private static final Color EVENT_DOT_COLOR = new Color(220, 50, 50);
    private static final Color EVENT_TEXT_COLOR = new Color(200, 30, 30);
    private static final Color WEEK_EVENT_BG = new Color(255, 235, 235);
    private static final Locale RUSSIAN = new Locale("ru");

    private static final String[] DAYS_OF_WEEK = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};

    /**
     * Генерирует PNG-картинку месячного календаря.
     * Дни с событиями отмечаются красным кружком.
     */
    public byte[] generateMonthImage(YearMonth month, List<Event> events) {
        Set<Integer> eventDays = events.stream()
                .map(e -> e.getDate().getDayOfMonth())
                .collect(Collectors.toSet());

        Map<Integer, List<Event>> eventsByDay = events.stream()
                .collect(Collectors.groupingBy(e -> e.getDate().getDayOfMonth()));

        BufferedImage image = new BufferedImage(MONTH_WIDTH, MONTH_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Фон
        g.setColor(BG_COLOR);
        g.fillRect(0, 0, MONTH_WIDTH, MONTH_HEIGHT);

        // Заголовок: "Апрель 2026"
        String title = capitalize(month.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, RUSSIAN))
                + " " + month.getYear();
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.setColor(HEADER_COLOR);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, (MONTH_WIDTH - fm.stringWidth(title)) / 2, 40);

        // Дни недели
        int cellW = MONTH_WIDTH / 7;
        int headerY = 70;
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(100, 100, 100));
        for (int i = 0; i < 7; i++) {
            String dayLabel = DAYS_OF_WEEK[i];
            fm = g.getFontMetrics();
            g.drawString(dayLabel, i * cellW + (cellW - fm.stringWidth(dayLabel)) / 2, headerY);
        }

        // Сетка
        int gridStartY = 85;
        int cellH = (MONTH_HEIGHT - gridStartY - 10) / 6;
        LocalDate firstDay = month.atDay(1);
        int startDow = firstDay.getDayOfWeek().getValue() - 1; // 0=Пн

        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            int pos = day - 1 + startDow;
            int col = pos % 7;
            int row = pos / 7;
            int x = col * cellW;
            int y = gridStartY + row * cellH;

            // Рамка ячейки
            g.setColor(GRID_COLOR);
            g.drawRect(x, y, cellW, cellH);

            // Число
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            String dayStr = String.valueOf(day);

            if (eventDays.contains(day)) {
                // Красный кружок
                int cx = x + cellW / 2;
                int cy = y + 18;
                g.setColor(EVENT_DOT_COLOR);
                g.fillOval(cx - 12, cy - 13, 24, 24);
                g.setColor(Color.WHITE);
            } else {
                g.setColor(DAY_COLOR);
            }
            fm = g.getFontMetrics();
            g.drawString(dayStr, x + (cellW - fm.stringWidth(dayStr)) / 2, y + 18);

            // Первое событие дня (краткое название)
            List<Event> dayEvents = eventsByDay.get(day);
            if (dayEvents != null && !dayEvents.isEmpty()) {
                g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g.setColor(EVENT_TEXT_COLOR);
                String eventTitle = truncate(dayEvents.getFirst().getTitle(), 12);
                fm = g.getFontMetrics();
                g.drawString(eventTitle, x + (cellW - fm.stringWidth(eventTitle)) / 2, y + 32);
            }
        }

        g.dispose();
        return toPng(image);
    }

    /**
     * Генерирует PNG-картинку недельного вида (7 дней начиная с from).
     * Дни с событиями выделяются красноватым фоном.
     */
    public byte[] generateWeekImage(LocalDate from, List<Event> events) {
        Map<LocalDate, List<Event>> eventsByDate = events.stream()
                .collect(Collectors.groupingBy(Event::getDate));

        BufferedImage image = new BufferedImage(WEEK_WIDTH, WEEK_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(BG_COLOR);
        g.fillRect(0, 0, WEEK_WIDTH, WEEK_HEIGHT);

        int cellW = WEEK_WIDTH / 7;
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("d MMM", RUSSIAN);

        for (int i = 0; i < 7; i++) {
            LocalDate day = from.plusDays(i);
            int x = i * cellW;
            List<Event> dayEvents = eventsByDate.getOrDefault(day, List.of());

            // Фон колонки
            if (!dayEvents.isEmpty()) {
                g.setColor(WEEK_EVENT_BG);
                g.fillRect(x, 0, cellW, WEEK_HEIGHT);
            }

            // Разделитель
            g.setColor(GRID_COLOR);
            g.drawLine(x, 0, x, WEEK_HEIGHT);

            // День недели
            String dowLabel = day.getDayOfWeek().getDisplayName(TextStyle.SHORT, RUSSIAN);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.setColor(HEADER_COLOR);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(capitalize(dowLabel), x + (cellW - fm.stringWidth(capitalize(dowLabel))) / 2, 22);

            // Дата
            String dateLabel = day.format(dayFmt);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            fm = g.getFontMetrics();
            g.drawString(dateLabel, x + (cellW - fm.stringWidth(dateLabel)) / 2, 40);

            // Горизонтальная линия под заголовком
            g.setColor(GRID_COLOR);
            g.drawLine(x, 48, x + cellW, 48);

            // События
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            int textY = 64;
            for (Event event : dayEvents) {
                g.setColor(EVENT_TEXT_COLOR);
                String label = truncate(event.getTitle(), 11);
                fm = g.getFontMetrics();
                g.drawString(label, x + (cellW - fm.stringWidth(label)) / 2, textY);
                textY += 16;
                if (textY > WEEK_HEIGHT - 10) break;
            }
        }

        g.dispose();
        return toPng(image);
    }

    private byte[] toPng(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode image to PNG", e);
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen - 1) + "…";
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
```

- [ ] **Step 5: Запустить тесты CalendarImageService**

```bash
./gradlew test --tests "com.studlgu.vkbot.service.handler.utils.CalendarImageServiceTest"
```

Ожидание: PASS

- [ ] **Step 6: Коммит**

```bash
git add src/main/java/com/studlgu/vkbot/service/handler/utils/CalendarImageService.java \
        src/main/resources/application.properties \
        src/test/java/com/studlgu/vkbot/service/handler/utils/CalendarImageServiceTest.java
git commit -m "feat: add CalendarImageService for PNG calendar generation"
```

---

## Task 6: StandardKeyboard + VkPhotoUploader

**Files:**
- Modify: `src/main/java/com/studlgu/vkbot/service/handler/utils/StandardKeyboard.java`
- Create: `src/main/java/com/studlgu/vkbot/service/handler/utils/VkPhotoUploader.java`

- [ ] **Step 1: Обновить StandardKeyboard.java**

Добавить кнопку «Календарь событий» в основную клавиатуру и два новых метода.

В методе `createkeyboard(Boolean isUserHasEditorRights)` добавить кнопку после кнопки «Получить меню»:

```java
keyboardButton.add(
        new KeyboardButton()
                .setAction(
                        new KeyboardButtonActionText()
                                .setLabel("\uD83D\uDCC5 Календарь событий")
                                .setPayload("{\"command\": \"calendar\"}")
                                .setType(KeyboardButtonActionTextType.TEXT)));
```

Добавить два новых static метода в конец класса:

```java
public static Keyboard createCalendarSubmenu() {
    List<List<KeyboardButton>> rows = new ArrayList<>();

    List<KeyboardButton> row1 = new ArrayList<>();
    row1.add(new KeyboardButton().setAction(new KeyboardButtonActionText()
            .setLabel("📅 Ближайшая неделя")
            .setPayload("{\"command\": \"calendar_week\"}")
            .setType(KeyboardButtonActionTextType.TEXT)));
    row1.add(new KeyboardButton().setAction(new KeyboardButtonActionText()
            .setLabel("📆 Этот месяц")
            .setPayload("{\"command\": \"calendar_month_current\"}")
            .setType(KeyboardButtonActionTextType.TEXT)));
    rows.add(row1);

    List<KeyboardButton> row2 = new ArrayList<>();
    row2.add(new KeyboardButton().setAction(new KeyboardButtonActionText()
            .setLabel("🗓 Выбрать месяц")
            .setPayload("{\"command\": \"calendar_month_select\"}")
            .setType(KeyboardButtonActionTextType.TEXT)));
    rows.add(row2);

    return new Keyboard().setButtons(rows).setInline(false);
}

public static Keyboard createMonthSelectKeyboard() {
    String[] months = {
        "Январь", "Февраль", "Март", "Апрель",
        "Май", "Июнь", "Июль", "Август",
        "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    };

    List<List<KeyboardButton>> rows = new ArrayList<>();
    for (int i = 0; i < 12; i += 3) {
        List<KeyboardButton> row = new ArrayList<>();
        for (int j = i; j < Math.min(i + 3, 12); j++) {
            int monthNum = j + 1;
            row.add(new KeyboardButton().setAction(new KeyboardButtonActionText()
                    .setLabel(months[j])
                    .setPayload("{\"command\": \"calendar_month\", \"month\": " + monthNum + "}")
                    .setType(KeyboardButtonActionTextType.TEXT)));
        }
        rows.add(row);
    }

    return new Keyboard().setButtons(rows).setInline(false);
}
```

- [ ] **Step 2: Создать VkPhotoUploader.java**

```java
package com.studlgu.vkbot.service.handler.utils;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.photos.responses.GetMessagesUploadServerResponse;
import com.vk.api.sdk.objects.photos.responses.PhotoUploadResponse;
import com.vk.api.sdk.objects.photos.responses.SaveMessagesPhotoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Component
@RequiredArgsConstructor
public class VkPhotoUploader {

    private final VkApiClient vkApiClient;

    /**
     * Загружает byte[] как фото в VK и возвращает attachment-строку вида "photoOWNER_ID".
     */
    public String uploadBytes(byte[] imageBytes, UserActor userActor)
            throws ApiException, ClientException, IOException {
        File tempFile = Files.createTempFile("vkbot_calendar_", ".png").toFile();
        try {
            Files.write(tempFile.toPath(), imageBytes);

            GetMessagesUploadServerResponse server = vkApiClient
                    .photos().getMessagesUploadServer(userActor).execute();

            PhotoUploadResponse uploaded = vkApiClient
                    .upload().photo(server.getUploadUrl().toURL().toString(), tempFile).execute();

            SaveMessagesPhotoResponse saved = vkApiClient
                    .photos().saveMessagesPhoto(userActor)
                    .photo(uploaded.getPhoto())
                    .server(uploaded.getServer())
                    .hash(uploaded.getHash())
                    .execute().getFirst();

            return "photo" + saved.getOwnerId() + "_" + saved.getId();
        } finally {
            tempFile.delete();
        }
    }
}
```

- [ ] **Step 3: Запустить все тесты**

```bash
./gradlew test
```

Ожидание: BUILD SUCCESS

- [ ] **Step 4: Коммит**

```bash
git add src/main/java/com/studlgu/vkbot/service/handler/utils/StandardKeyboard.java \
        src/main/java/com/studlgu/vkbot/service/handler/utils/VkPhotoUploader.java
git commit -m "feat: add calendar buttons to StandardKeyboard and VkPhotoUploader helper"
```

---

## Task 7: View CommandHandlers (просмотр календаря)

**Files:**
- Create: `src/main/java/com/studlgu/vkbot/service/handler/command/impl/CalendarCommandHandler.java`
- Create: `src/main/java/com/studlgu/vkbot/service/handler/command/impl/CalendarWeekCommandHandler.java`
- Create: `src/main/java/com/studlgu/vkbot/service/handler/command/impl/CalendarMonthCurrentCommandHandler.java`
- Create: `src/main/java/com/studlgu/vkbot/service/handler/command/impl/CalendarMonthSelectCommandHandler.java`
- Create: `src/main/java/com/studlgu/vkbot/service/handler/command/impl/CalendarMonthCommandHandler.java`

- [ ] **Step 1: Создать CalendarCommandHandler.java**

Обрабатывает команду `CALENDAR` — отправляет подменю.

```java
package com.studlgu.vkbot.service.handler.command.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.command.CommandHandler;
import com.studlgu.vkbot.service.handler.command.CommandType;
import com.studlgu.vkbot.service.handler.utils.StandardKeyboard;
import com.studlgu.vkbot.service.handler.utils.VkActorFactory;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@RequiredArgsConstructor
public class CalendarCommandHandler implements CommandHandler {

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;

    @Override
    public CommandType getType() {
        return CommandType.CALENDAR;
    }

    @Override
    public void handle(CallbackRequest request) {
        UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());
        try {
            vkApiClient.messages().sendDeprecated(userActor)
                    .message("Выберите период:")
                    .keyboard(StandardKeyboard.createCalendarSubmenu())
                    .userId(userActor.getId())
                    .randomId(Math.abs(new Random().nextInt(10000)))
                    .execute();
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }
    }
}
```

- [ ] **Step 2: Создать CalendarWeekCommandHandler.java**

```java
package com.studlgu.vkbot.service.handler.command.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.model.Event;
import com.studlgu.vkbot.service.handler.command.CommandHandler;
import com.studlgu.vkbot.service.handler.command.CommandType;
import com.studlgu.vkbot.service.handler.utils.*;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class CalendarWeekCommandHandler implements CommandHandler {

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;
    private final RoleIdentifier roleIdentifier;
    private final EventService eventService;
    private final CalendarImageService imageService;
    private final VkPhotoUploader photoUploader;

    @Override
    public CommandType getType() {
        return CommandType.CALENDAR_WEEK;
    }

    @Override
    public void handle(CallbackRequest request) {
        UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());
        try {
            LocalDate today = LocalDate.now();
            List<Event> events = eventService.getEventsForWeek(today);
            byte[] png = imageService.generateWeekImage(today, events);
            String attachment = photoUploader.uploadBytes(png, userActor);

            vkApiClient.messages().sendDeprecated(userActor)
                    .message("📅 События ближайшей недели:")
                    .attachment(attachment)
                    .keyboard(StandardKeyboard.createkeyboard(roleIdentifier.hasEditorRights(vkApiClient, userActor)))
                    .userId(userActor.getId())
                    .randomId(Math.abs(new Random().nextInt(10000)))
                    .execute();
        } catch (ApiException | ClientException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
```

- [ ] **Step 3: Создать CalendarMonthCurrentCommandHandler.java**

```java
package com.studlgu.vkbot.service.handler.command.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.model.Event;
import com.studlgu.vkbot.service.handler.command.CommandHandler;
import com.studlgu.vkbot.service.handler.command.CommandType;
import com.studlgu.vkbot.service.handler.utils.*;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.YearMonth;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class CalendarMonthCurrentCommandHandler implements CommandHandler {

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;
    private final RoleIdentifier roleIdentifier;
    private final EventService eventService;
    private final CalendarImageService imageService;
    private final VkPhotoUploader photoUploader;

    @Override
    public CommandType getType() {
        return CommandType.CALENDAR_MONTH_CURRENT;
    }

    @Override
    public void handle(CallbackRequest request) {
        UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());
        try {
            YearMonth currentMonth = YearMonth.now();
            List<Event> events = eventService.getEventsForMonth(currentMonth);
            byte[] png = imageService.generateMonthImage(currentMonth, events);
            String attachment = photoUploader.uploadBytes(png, userActor);

            vkApiClient.messages().sendDeprecated(userActor)
                    .message("📆 События текущего месяца:")
                    .attachment(attachment)
                    .keyboard(StandardKeyboard.createkeyboard(roleIdentifier.hasEditorRights(vkApiClient, userActor)))
                    .userId(userActor.getId())
                    .randomId(Math.abs(new Random().nextInt(10000)))
                    .execute();
        } catch (ApiException | ClientException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
```

- [ ] **Step 4: Создать CalendarMonthSelectCommandHandler.java**

```java
package com.studlgu.vkbot.service.handler.command.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.command.CommandHandler;
import com.studlgu.vkbot.service.handler.command.CommandType;
import com.studlgu.vkbot.service.handler.utils.StandardKeyboard;
import com.studlgu.vkbot.service.handler.utils.VkActorFactory;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@RequiredArgsConstructor
public class CalendarMonthSelectCommandHandler implements CommandHandler {

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;

    @Override
    public CommandType getType() {
        return CommandType.CALENDAR_MONTH_SELECT;
    }

    @Override
    public void handle(CallbackRequest request) {
        UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());
        try {
            vkApiClient.messages().sendDeprecated(userActor)
                    .message("Выберите месяц:")
                    .keyboard(StandardKeyboard.createMonthSelectKeyboard())
                    .userId(userActor.getId())
                    .randomId(Math.abs(new Random().nextInt(10000)))
                    .execute();
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }
    }
}
```

- [ ] **Step 5: Создать CalendarMonthCommandHandler.java**

```java
package com.studlgu.vkbot.service.handler.command.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.model.Event;
import com.studlgu.vkbot.service.handler.command.CommandHandler;
import com.studlgu.vkbot.service.handler.command.CommandType;
import com.studlgu.vkbot.service.handler.utils.*;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.YearMonth;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class CalendarMonthCommandHandler implements CommandHandler {

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;
    private final RoleIdentifier roleIdentifier;
    private final EventService eventService;
    private final CalendarImageService imageService;
    private final VkPhotoUploader photoUploader;

    @Override
    public CommandType getType() {
        return CommandType.CALENDAR_MONTH;
    }

    @Override
    public void handle(CallbackRequest request) {
        UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());
        try {
            Integer monthNum = request.getObject().getMessage().getMappedPayload().getMonth();
            if (monthNum == null || monthNum < 1 || monthNum > 12) {
                monthNum = YearMonth.now().getMonthValue();
            }
            YearMonth month = YearMonth.of(YearMonth.now().getYear(), monthNum);
            List<Event> events = eventService.getEventsForMonth(month);
            byte[] png = imageService.generateMonthImage(month, events);
            String attachment = photoUploader.uploadBytes(png, userActor);

            vkApiClient.messages().sendDeprecated(userActor)
                    .message("🗓 События за выбранный месяц:")
                    .attachment(attachment)
                    .keyboard(StandardKeyboard.createkeyboard(roleIdentifier.hasEditorRights(vkApiClient, userActor)))
                    .userId(userActor.getId())
                    .randomId(Math.abs(new Random().nextInt(10000)))
                    .execute();
        } catch (ApiException | ClientException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
```

- [ ] **Step 6: Запустить все тесты**

```bash
./gradlew test
```

Ожидание: BUILD SUCCESS (новые handler'ы не имеют unit-тестов, но компилируются)

- [ ] **Step 7: Коммит**

```bash
git add src/main/java/com/studlgu/vkbot/service/handler/command/impl/Calendar*.java
git commit -m "feat: add calendar view command handlers"
```

---

## Task 8: Editor CommandHandlers (управление событиями)

**Files:**
- Create: `src/main/java/com/studlgu/vkbot/service/handler/command/impl/AddEventCommandHandler.java`
- Create: `src/main/java/com/studlgu/vkbot/service/handler/command/impl/DeleteEventCommandHandler.java`

> Эти handler'ы видны только редакторам. Кнопки добавляются в `StandardKeyboard.createkeyboard()` аналогично «Загрузить меню».

- [ ] **Step 1: Добавить кнопки редактора в StandardKeyboard**

В метод `addEditorButtons` добавить кнопки после «Загрузить меню»:

```java
keyboardButton.add(
        new KeyboardButton()
                .setAction(
                        new KeyboardButtonActionText()
                                .setLabel("➕ Добавить событие")
                                .setPayload("{\"command\": \"add_event\"}")
                                .setType(KeyboardButtonActionTextType.TEXT)));

keyboardButton.add(
        new KeyboardButton()
                .setAction(
                        new KeyboardButtonActionText()
                                .setLabel("🗑 Удалить событие")
                                .setPayload("{\"command\": \"delete_event\"}")
                                .setType(KeyboardButtonActionTextType.TEXT)));
```

- [ ] **Step 2: Создать AddEventCommandHandler.java**

```java
package com.studlgu.vkbot.service.handler.command.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.command.CommandHandler;
import com.studlgu.vkbot.service.handler.command.CommandType;
import com.studlgu.vkbot.service.handler.utils.UserState;
import com.studlgu.vkbot.service.handler.utils.UserStateCache;
import com.studlgu.vkbot.service.handler.utils.VkActorFactory;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@RequiredArgsConstructor
public class AddEventCommandHandler implements CommandHandler {

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;
    private final UserStateCache userStateCache;

    @Override
    public CommandType getType() {
        return CommandType.ADD_EVENT;
    }

    @Override
    public void handle(CallbackRequest request) {
        UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());
        try {
            userStateCache.setState(userActor.getId(), UserState.AWAITING_ADD_EVENT);
            vkApiClient.messages().sendDeprecated(userActor)
                    .message("Отправь событие в формате:\n" +
                             "название|YYYY-MM-DD|HH:mm|описание|место\n\n" +
                             "Пример:\nЭкзамен по математике|2026-05-15|09:00|Билеты 1-40|Корпус А, ауд. 301")
                    .userId(userActor.getId())
                    .randomId(Math.abs(new Random().nextInt(10000)))
                    .execute();
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }
    }
}
```

- [ ] **Step 3: Создать DeleteEventCommandHandler.java**

```java
package com.studlgu.vkbot.service.handler.command.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.model.Event;
import com.studlgu.vkbot.service.handler.command.CommandHandler;
import com.studlgu.vkbot.service.handler.command.CommandType;
import com.studlgu.vkbot.service.handler.utils.EventService;
import com.studlgu.vkbot.service.handler.utils.UserState;
import com.studlgu.vkbot.service.handler.utils.UserStateCache;
import com.studlgu.vkbot.service.handler.utils.VkActorFactory;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class DeleteEventCommandHandler implements CommandHandler {

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;
    private final EventService eventService;
    private final UserStateCache userStateCache;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Override
    public CommandType getType() {
        return CommandType.DELETE_EVENT;
    }

    @Override
    public void handle(CallbackRequest request) {
        UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());
        try {
            List<Event> events = eventService.listAll();

            if (events.isEmpty()) {
                vkApiClient.messages().sendDeprecated(userActor)
                        .message("Нет событий для удаления.")
                        .userId(userActor.getId())
                        .randomId(Math.abs(new Random().nextInt(10000)))
                        .execute();
                return;
            }

            StringBuilder sb = new StringBuilder("Отправь ID события для удаления:\n\n");
            for (Event event : events) {
                sb.append("ID: ").append(event.getId(), 0, 8).append("...\n")
                  .append("  ").append(event.getTitle())
                  .append(" — ").append(event.getDate().format(DATE_FMT))
                  .append("\n\n");
            }

            userStateCache.setState(userActor.getId(), UserState.AWAITING_DELETE_ID);

            vkApiClient.messages().sendDeprecated(userActor)
                    .message(sb.toString())
                    .userId(userActor.getId())
                    .randomId(Math.abs(new Random().nextInt(10000)))
                    .execute();
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }
    }
}
```

> Примечание: ID показывается сокращённым (`id.substring(0, 8) + "..."`), чтобы пользователь мог отправить только первые 8 символов. `DeleteEventInputHandler` должен искать событие по совпадению начала ID.

- [ ] **Step 4: Запустить все тесты**

```bash
./gradlew test
```

Ожидание: BUILD SUCCESS

- [ ] **Step 5: Коммит**

```bash
git add src/main/java/com/studlgu/vkbot/service/handler/command/impl/AddEventCommandHandler.java \
        src/main/java/com/studlgu/vkbot/service/handler/command/impl/DeleteEventCommandHandler.java \
        src/main/java/com/studlgu/vkbot/service/handler/utils/StandardKeyboard.java
git commit -m "feat: add editor command handlers for event management"
```

---

## Task 9: Обновить EventRepository.deleteById для поиска по префиксу

**Files:**
- Modify: `src/main/java/com/studlgu/vkbot/service/handler/utils/EventRepository.java`
- Modify: `src/test/java/com/studlgu/vkbot/service/handler/utils/EventRepositoryTest.java`

Т.к. в `DeleteEventCommandHandler` пользователь отправляет первые 8 символов UUID, нужна поддержка удаления по префиксу ID.

- [ ] **Step 1: Добавить тест на deleteByIdPrefix**

В `EventRepositoryTest.java` добавить:

```java
@Test
void deleteByIdPrefix_matchesAndRemoves() {
    Event event = new Event(null, "Тест", LocalDate.of(2026, 5, 1),
            LocalTime.of(10, 0), "desc", "loc");
    repository.save(event);
    String fullId = repository.findAll().getFirst().getId();
    String prefix = fullId.substring(0, 8);

    boolean deleted = repository.deleteByIdPrefix(prefix);

    assertThat(deleted).isTrue();
    assertThat(repository.findAll()).isEmpty();
}
```

- [ ] **Step 2: Запустить тест — убедиться что падает**

```bash
./gradlew test --tests "com.studlgu.vkbot.service.handler.utils.EventRepositoryTest"
```

Ожидание: FAIL

- [ ] **Step 3: Добавить метод deleteByIdPrefix в EventRepository.java**

```java
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
```

- [ ] **Step 4: Обновить EventService — добавить deleteEventByPrefix**

```java
public boolean deleteEventByPrefix(String idPrefix) {
    return repository.deleteByIdPrefix(idPrefix);
}
```

- [ ] **Step 5: Запустить тесты**

```bash
./gradlew test --tests "com.studlgu.vkbot.service.handler.utils.EventRepositoryTest"
```

Ожидание: PASS

- [ ] **Step 6: Коммит**

```bash
git add src/main/java/com/studlgu/vkbot/service/handler/utils/EventRepository.java \
        src/main/java/com/studlgu/vkbot/service/handler/utils/EventService.java \
        src/test/java/com/studlgu/vkbot/service/handler/utils/EventRepositoryTest.java
git commit -m "feat: add deleteByIdPrefix to EventRepository for short-ID deletion"
```

---

## Task 10: State-based routing + input handlers

**Files:**
- Modify: `src/main/java/com/studlgu/vkbot/service/handler/callback/CallbackService.java`
- Create: `src/main/java/com/studlgu/vkbot/service/handler/callback/impl/AddEventInputHandler.java`
- Create: `src/main/java/com/studlgu/vkbot/service/handler/callback/impl/DeleteEventInputHandler.java`

- [ ] **Step 1: Убедиться что CallbackType содержит новые значения из Task 3**

`CallbackType` должен содержать `ADD_EVENT_INPUT("add_event_input", "addEventInputHandler")` и `DELETE_EVENT_INPUT("delete_event_input", "deleteEventInputHandler")` — добавлены в Task 3 Step 3.

- [ ] **Step 2: Обновить CallbackService.java**

Инжектировать `UserStateCache` и добавить проверку состояния в `defineType()`:

```java
@Service
@RequiredArgsConstructor
public class CallbackService {

    private final ApplicationContext applicationContext;
    private final UserStateCache userStateCache;  // добавить

    public String handle(CallbackRequest request) {
        Optional<CallbackType> callbackType = CallbackType.findByType(defineType(request));
        if (callbackType.isPresent()) {
            ICallbackHandler handler = applicationContext.getBean(callbackType.get().getHandlerName(), ICallbackHandler.class);
            return handler.handle(request);
        }
        return "callback was not recognized";
    }

    public String defineType(CallbackRequest request) {
        List<CallbackAttachment> attachments = Optional.ofNullable(request.getObject())
                .map(CallbackObject::getMessage)
                .map(CallbackMessage::getAttachments)
                .orElse(null);

        if (attachments != null && !attachments.isEmpty()) {
            boolean isAllPhoto = attachments.stream()
                    .allMatch(a -> "photo".equals(a.getType()));
            if (isAllPhoto) return "upload_photo";
        }

        // Проверяем состояние пользователя для текстовых ответов
        if ("message_new".equals(request.getType())) {
            Long userId = Optional.ofNullable(request.getObject())
                    .map(CallbackObject::getMessage)
                    .map(CallbackMessage::getFromId)
                    .orElse(null);
            if (userId != null) {
                Optional<UserState> state = userStateCache.getState(userId);
                if (state.isPresent()) {
                    return switch (state.get()) {
                        case AWAITING_ADD_EVENT -> "add_event_input";
                        case AWAITING_DELETE_ID -> "delete_event_input";
                        default -> request.getType();
                    };
                }
            }
        }

        return request.getType();
    }
}
```

Добавить импорт: `import com.studlgu.vkbot.service.handler.utils.UserState;` и `import com.studlgu.vkbot.service.handler.utils.UserStateCache;`

- [ ] **Step 3: Создать AddEventInputHandler.java**

```java
package com.studlgu.vkbot.service.handler.callback.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.callback.ICallbackHandler;
import com.studlgu.vkbot.service.handler.utils.*;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component("addEventInputHandler")
@RequiredArgsConstructor
public class AddEventInputHandler implements ICallbackHandler {

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;
    private final RoleIdentifier roleIdentifier;
    private final EventService eventService;
    private final UserStateCache userStateCache;

    @Override
    public String handle(CallbackRequest request) {
        UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());
        String text = request.getObject().getMessage().getText();
        try {
            eventService.addEvent(text);
            userStateCache.clearState(userActor.getId());
            vkApiClient.messages().sendDeprecated(userActor)
                    .message("✅ Событие добавлено!")
                    .keyboard(StandardKeyboard.createkeyboard(roleIdentifier.hasEditorRights(vkApiClient, userActor)))
                    .userId(userActor.getId())
                    .randomId(Math.abs(new Random().nextInt(10000)))
                    .execute();
        } catch (IllegalArgumentException e) {
            try {
                vkApiClient.messages().sendDeprecated(userActor)
                        .message("❌ Ошибка: " + e.getMessage())
                        .userId(userActor.getId())
                        .randomId(Math.abs(new Random().nextInt(10000)))
                        .execute();
            } catch (ApiException | ClientException ex) {
                throw new RuntimeException(ex);
            }
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }
        return "ok";
    }
}
```

> Примечание: `CallbackMessage` должна иметь поле `text`. Проверить в `CallbackMessage.java` — если отсутствует, добавить `private String text;` с `@Getter @Setter`.

- [ ] **Step 4: Убедиться что поле text в CallbackMessage.java присутствует**

Поле `private String text;` уже существует в `CallbackMessage.java` — изменений не требуется.

- [ ] **Step 5: Создать DeleteEventInputHandler.java**

```java
package com.studlgu.vkbot.service.handler.callback.impl;

import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.callback.ICallbackHandler;
import com.studlgu.vkbot.service.handler.utils.*;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component("deleteEventInputHandler")
@RequiredArgsConstructor
public class DeleteEventInputHandler implements ICallbackHandler {

    private final VkApiClient vkApiClient;
    private final VkActorFactory actorFactory;
    private final RoleIdentifier roleIdentifier;
    private final EventService eventService;
    private final UserStateCache userStateCache;

    @Override
    public String handle(CallbackRequest request) {
        UserActor userActor = actorFactory.create(request.getObject().getMessage().getFromId());
        String idPrefix = request.getObject().getMessage().getText().trim();
        try {
            boolean deleted = eventService.deleteEventByPrefix(idPrefix);
            userStateCache.clearState(userActor.getId());
            String message = deleted
                    ? "✅ Событие удалено!"
                    : "❌ Событие с таким ID не найдено.";
            vkApiClient.messages().sendDeprecated(userActor)
                    .message(message)
                    .keyboard(StandardKeyboard.createkeyboard(roleIdentifier.hasEditorRights(vkApiClient, userActor)))
                    .userId(userActor.getId())
                    .randomId(Math.abs(new Random().nextInt(10000)))
                    .execute();
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }
        return "ok";
    }
}
```

- [ ] **Step 6: Запустить все тесты**

```bash
./gradlew test
```

Ожидание: BUILD SUCCESS

- [ ] **Step 7: Запустить сборку**

```bash
./gradlew build
```

Ожидание: BUILD SUCCESS

- [ ] **Step 8: Коммит**

```bash
git add src/main/java/com/studlgu/vkbot/service/handler/callback/CallbackService.java \
        src/main/java/com/studlgu/vkbot/service/handler/callback/impl/AddEventInputHandler.java \
        src/main/java/com/studlgu/vkbot/service/handler/callback/impl/DeleteEventInputHandler.java \
        src/main/java/com/studlgu/vkbot/model/CallbackMessage.java
git commit -m "feat: add state-based routing and input handlers for event management"
```

---

## Итоговая проверка

- [ ] Запустить полный build:

```bash
./gradlew build
```

Ожидание: BUILD SUCCESS, все тесты зелёные.

- [ ] Проверить список новых файлов:

```bash
git diff --name-only HEAD~10 HEAD
```

Убедиться, что все файлы из карты файлов присутствуют.
