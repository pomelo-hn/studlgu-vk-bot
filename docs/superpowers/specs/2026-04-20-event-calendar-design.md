# Дизайн: Календарь событий

**Дата:** 2026-04-20  
**Проект:** studlgu-vk-bot  
**Статус:** Согласован

---

## Контекст

Бот для студенческой группы. Необходимо добавить функцию просмотра событий (экзамены, дедлайны, мероприятия) в виде картинки-календаря. Редакторы группы управляют событиями, все пользователи могут их просматривать.

---

## Модель данных

### Класс `Event`

| Поле | Тип | Описание |
|---|---|---|
| `id` | `String` (UUID) | Уникальный идентификатор, генерируется при создании |
| `title` | `String` | Название события |
| `date` | `LocalDate` | Дата (YYYY-MM-DD) |
| `time` | `LocalTime` | Время (HH:mm) |
| `description` | `String` | Описание |
| `location` | `String` | Место проведения |

### Хранение

Файл `events.json` по пути из `vkbot.events.storage.path`.

```json
[
  {
    "id": "a1b2c3d4-...",
    "title": "Экзамен по математике",
    "date": "2026-05-15",
    "time": "09:00",
    "description": "Билеты 1-40",
    "location": "Корпус А, ауд. 301"
  }
]
```

### Формат добавления (редактор)

Одним сообщением в формате:
```
название|YYYY-MM-DD|HH:mm|описание|место
```

Пример: `Экзамен по математике|2026-05-15|09:00|Билеты 1-40|Корпус А, ауд. 301`

---

## Архитектура

### Новые компоненты

```
model/
  Event.java                          — POJO события

service/handler/utils/
  EventRepository.java                — чтение/запись events.json (Jackson + ReentrantReadWriteLock)
  EventService.java                   — бизнес-логика: фильтрация, добавление, удаление
  CalendarImageService.java           — генерация PNG через Java AWT/Graphics2D

service/handler/command/impl/
  CalendarCommandHandler.java         — отправляет подменю-клавиатуру
  CalendarWeekCommandHandler.java     — картинка: ближайшие 7 дней
  CalendarMonthCurrentCommandHandler  — картинка: текущий месяц
  CalendarMonthSelectCommandHandler   — отправляет клавиатуру выбора месяца
  CalendarMonthCommandHandler.java    — картинка: месяц из payload.month
  AddEventCommandHandler.java         — парсинг строки, добавление события
  DeleteEventCommandHandler.java      — показ списка с ID, установка состояния ожидания
```

### Изменения в существующем коде

| Файл | Изменение |
|---|---|
| `CommandType.java` | Добавить: `CALENDAR`, `CALENDAR_WEEK`, `CALENDAR_MONTH_CURRENT`, `CALENDAR_MONTH_SELECT`, `CALENDAR_MONTH`, `ADD_EVENT`, `DELETE_EVENT` |
| `Payload.java` | Добавить поле `month` (Integer) — используется для `CALENDAR_MONTH` |
| `StandardKeyboard.java` | Добавить кнопку «Календарь событий»; новый метод `createCalendarSubmenu()`; новый метод `createMonthSelectKeyboard()` |
| `UserStateCache.java` | Добавить состояния `AWAITING_DELETE_ID` и `AWAITING_ADD_EVENT` |
| `application.properties` | Новый параметр `vkbot.events.storage.path` |

---

## UX-поток

### Просмотр событий

```
Главная клавиатура
  └── [Календарь событий]  →  CALENDAR
          ↓
  Подменю (inline-клавиатура):
  ├── [Ближайшая неделя]    →  CALENDAR_WEEK            →  PNG (горизонтальная полоса 7 дней)
  ├── [Этот месяц]          →  CALENDAR_MONTH_CURRENT   →  PNG (сетка текущего месяца)
  └── [Выбрать месяц]       →  CALENDAR_MONTH_SELECT
              ↓
      Клавиатура с 12 кнопками (Январь–Декабрь текущего года)
      payload: {"command": "calendar_month", "month": 5}
              ↓
      CALENDAR_MONTH (месяц из payload.month)    →  PNG
```

### Управление событиями (только редакторы)

**Добавление:**
```
[Добавить событие]  →  ADD_EVENT
  Бот: «Отправь событие в формате: название|YYYY-MM-DD|HH:mm|описание|место»
  Пользователь: отправляет строку  →  UserStateCache.setAwaitingAddEvent(userId)
  Бот: парсит, сохраняет, подтверждает
```

**Удаление:**
```
[Удалить событие]  →  DELETE_EVENT
  Бот: присылает нумерованный список событий с их ID
  UserStateCache.setAwaitingDeleteId(userId)
  Пользователь: отвечает ID  →  событие удаляется
  Бот: подтверждает удаление
```

---

## CalendarImageService

### Режим «месяц» (PNG, ~800×600px)

- Заголовок: название месяца и год
- Сетка 7 колонок (Пн–Вс) × 6 строк
- Дни с событиями: красный кружок на числе
- Под числом — краткое название события (если помещается)
- Реализация: `java.awt.Graphics2D` → `BufferedImage` → `byte[]` (PNG)

### Режим «неделя» (PNG, ~800×300px)

- Горизонтальная полоса: 7 колонок (текущий день + 6 следующих)
- Каждая колонка: день недели, число, список событий дня
- Дни с событиями выделяются красным фоном колонки

### Загрузка в VK

1. `photos.getMessagesUploadServer` — получить URL для загрузки
2. Загрузить `byte[]` через `RestClient`
3. `photos.saveMessagesPhoto` — сохранить фото
4. Отправить сообщение с прикреплённым фото

---

## Конфигурация

```properties
vkbot.events.storage.path=   # Путь к файлу events.json
```

---

## EventRepository

- Использует `Jackson ObjectMapper` для сериализации/десериализации
- `ReentrantReadWriteLock` — по аналогии с `PhotoStorage`
- Методы: `findAll()`, `save(Event)`, `deleteById(String)`

---

## EventService

- `getEventsForWeek(LocalDate from)` — события на 7 дней вперёд
- `getEventsForMonth(YearMonth)` — все события за месяц
- `addEvent(String rawInput)` — парсинг формата `название|дата|время|описание|место`, валидация, сохранение
- `deleteEvent(String id)` — удаление по ID, возвращает `boolean` (найдено/нет)
- `listAll()` — полный список для отображения редактору перед удалением
