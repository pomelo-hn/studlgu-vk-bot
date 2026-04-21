# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build

# Run
./gradlew bootRun

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.studlgu.vkbot.VkBotApplicationTests"
```

## Tech Stack

- Java 21, Spring Boot 4.0.3, Gradle (`spring-boot-starter-webmvc`, не reactive)
- VK Java SDK 1.0.16 для работы с VK API
- Lombok для генерации boilerplate
- `UserStateCache` использует in-memory `ConcurrentHashMap` (Redis сконфигурирован в `application.properties`, но не используется)

## Architecture

Бот обрабатывает входящие Callback-события от VK через единственный endpoint `POST /callback`.

### Поток обработки событий

```
CallbackController → CallbackService → ICallbackHandler → CommandHandlerService → CommandHandler
```

1. `CallbackController` десериализует `CallbackRequest`, парсит JSON из поля `payload` в `Payload`-объект.
2. `CallbackService.defineType()` определяет тип события: если сообщение содержит только фото — возвращает `"upload_photo"`, иначе возвращает `request.getType()`.
3. `CallbackType` (enum) сопоставляет тип события с именем Spring-бина обработчика.
4. Нужный `ICallbackHandler` получается из `ApplicationContext` по имени бина.

### Callback-обработчики (`ICallbackHandler`)

| Бин | Тип события | Назначение |
|-----|-------------|------------|
| `confirmationHandler` | `confirmation` | Возвращает код подтверждения VK вебхука |
| `messageNewHandler` | `message_new` | Читает команду из `mappedPayload.command`, делегирует в `CommandHandlerService` |
| `messageEventHandler` | `message_event` | **TODO: deprecated** — читает команду из `object.payload.command` |
| `messageReplyHandler` | `message_reply` | Заглушка, всегда возвращает `"ok"` |
| `uploadPhotoHandler` | `upload_photo` | Сохраняет фото меню (только если пользователь в состоянии ожидания) |

### Команды (`CommandHandler`)

Каждая реализация `CommandHandler` объявляет свой `CommandType` через `getType()`. `CommandHandlerService` регистрирует их в `Map<CommandType, CommandHandler>` при старте.

| Команда | Обработчик |
|---------|------------|
| `START` | Отправляет клавиатуру |
| `MOTIVATION` | Мотивационное сообщение |
| `BELL_SCHEDULE` | Расписание звонков |
| `WHICH_WEEK` | Верхняя/нижняя неделя (от `vkbot.week-start-date`) |
| `GET_MENU` | Загружает фото из `PhotoStorage` в VK и отправляет пользователю |
| `UPLOAD_MENU` | Устанавливает состояние ожидания фото в `UserStateCache`, после чего бот ждёт фото от пользователя |

### Конфигурация

- **`VkConfig`** — создаёт `VkApiClient` с HTTP-транспортом, устанавливает хост `api.vk.ru`.

### Утилиты

- **`VkActorFactory`** — создаёт `UserActor(userId, accessToken)` для вызовов VK API.
- **`RoleIdentifier`** — проверяет, есть ли у пользователя роль из `vkbot.editor-roles` в группе. Используется для показа кнопки «Загрузить меню».
- **`StandardKeyboard`** — собирает VK-клавиатуру. Принимает `isUserHasEditorRights`: при `true` добавляет кнопку «Загрузить меню».
- **`UserStateCache`** — in-memory TTL-кэш (TTL 10 мин, очистка каждые 5 мин). Хранит, у каких пользователей бот ожидает фото.
- **`PhotoStorage`** — файловое хранилище для фото меню в директории `vkbot.photo.storage.path`. При сохранении новых фото удаляет старые. Использует `ReentrantReadWriteLock`.

### Сценарий загрузки меню

1. Пользователь-редактор нажимает «Загрузить меню» → `UploadMenuCommandHandler` вызывает `userStateCache.setWaitingPhoto(userId)`.
2. Пользователь отправляет фото → `CallbackService.defineType()` определяет тип `upload_photo` → `UploadPhotoHandler`.
3. `UploadPhotoHandler` проверяет `userStateCache.isWaitingPhoto()`, скачивает фото через `RestClient`, сохраняет через `PhotoStorage`, сбрасывает состояние.

## Configuration Properties

```properties
vkbot.access-token=          # VK User Access Token
vkbot.group-id=              # ID группы ВКонтакте
vkbot.confirmation-code=     # Код подтверждения Callback API
vkbot.week-start-date=       # Дата начала учебного года (YYYY-MM-DD), от неё считается чётность недели
vkbot.editor-roles=          # Роли с правами редактора (через ;), например: administrator;moderator;editor;creator
vkbot.photo.storage.path=    # Путь к директории для хранения фото меню
```
