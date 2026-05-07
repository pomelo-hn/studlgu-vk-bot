# Инструкции для работы с репозиторием

## Проект

`vk-bot` - Java/Spring Boot приложение для VK Callback API. Бот принимает события VK через единый HTTP endpoint и обрабатывает команды пользователя, меню, календарь событий и загрузку фотографий.

## Стек

- Java 21
- Spring Boot 4.0.3
- Gradle Wrapper
- VK Java SDK `com.vk.api:sdk:1.0.16`
- Lombok
- Jackson
- JUnit Platform для тестов

## Основные команды

Для Windows/PowerShell:

```powershell
.\gradlew.bat build
.\gradlew.bat test
.\gradlew.bat bootRun
```

Для Unix-like окружения:

```bash
./gradlew build
./gradlew test
./gradlew bootRun
```

Запуск одного тестового класса:

```powershell
.\gradlew.bat test --tests "com.studlgu.vkbot.VkBotApplicationTests"
```

## Архитектура

Основной поток обработки:

```text
CallbackController -> CallbackService -> ICallbackHandler -> CommandHandlerService -> CommandHandler
```

- `CallbackController` принимает `POST /callback`, десериализует запрос и payload.
- `CallbackService` определяет тип callback-события и выбирает Spring bean обработчика.
- `ICallbackHandler` обрабатывает конкретный тип события VK.
- `CommandHandlerService` маршрутизирует команды к реализациям `CommandHandler`.
- Команды описаны через `CommandType`; каждый обработчик возвращает свой тип через `getType()`.

## Важные директории и файлы

- `src/main/java/com/studlgu/vkbot/controller` - HTTP controller.
- `src/main/java/com/studlgu/vkbot/service/handler/callback` - обработка callback-событий VK.
- `src/main/java/com/studlgu/vkbot/service/handler/command` - обработка команд бота.
- `src/main/java/com/studlgu/vkbot/service/handler/utils` - вспомогательные сервисы: клавиатура, роли, состояние пользователя, фото, события, календарь.
- `src/main/java/com/studlgu/vkbot/model` - DTO callback-запросов и payload.
- `src/main/resources/application.properties` - конфигурация приложения.
- `events.json` - файловое хранилище событий.
- `menu_photos` - локальное хранилище фотографий меню.

## Конфигурация

Ключевые свойства:

```properties
vkbot.group-id=
vkbot.confirmation-code=
vkbot.access-token=
vkbot.week-start-date=
vkbot.editor-roles=
vkbot.photo.storage.path=
vkbot.events.storage.path=
```

Важно: токены и секреты не стоит хранить в репозитории. При изменениях конфигурации предпочтительно выносить секреты в переменные окружения, параметры запуска или локальные ignored-файлы.

## Практические правила

- Сохранять существующий стиль Spring services/handlers и не вводить новые архитектурные слои без необходимости.
- Для новой команды добавлять значение в `CommandType`, реализацию `CommandHandler` и, если нужно, кнопку в `StandardKeyboard`.
- Для нового callback-типа добавлять значение в `CallbackType` и реализацию `ICallbackHandler`.
- После изменений в бизнес-логике запускать `.\gradlew.bat test`.
- Не трогать пользовательские изменения в рабочем дереве, если они не относятся к текущей задаче.
- Не коммитить build artifacts и локальные IDE-файлы.

## Текущее базовое состояние

На момент инициализации `.\gradlew.bat test` проходит успешно.
