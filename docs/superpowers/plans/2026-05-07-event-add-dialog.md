# Event Add Dialog Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace pipe-separated event creation with a cancellable step-by-step dialog where only title and date are required.

**Architecture:** Add event-specific states to `UserState`, store partial event data in a small `EventDraftCache`, and route all add-event states to the existing `AddEventInputHandler`. Update `EventService` and event display/sorting to support optional time, description, and location.

**Tech Stack:** Java 21, Spring Boot 4.0.3, JUnit Platform, Mockito, AssertJ, VK Java SDK.

---

### Task 1: Event Service Optional Fields

**Files:**
- Modify: `src/main/java/com/studlgu/vkbot/service/handler/utils/EventService.java`
- Test: `src/test/java/com/studlgu/vkbot/service/handler/utils/EventServiceTest.java`

- [ ] Write failing tests for saving required-only events and untimed sorting.
- [ ] Run `.\gradlew.bat test --tests "com.studlgu.vkbot.service.handler.utils.EventServiceTest"` and verify failure.
- [ ] Add an `addEvent(String title, LocalDate date, LocalTime time, String description, String location)` method.
- [ ] Normalize blank optional fields to `null`; keep title/date required.
- [ ] Sort null times last with `Comparator.nullsLast`.
- [ ] Run the EventService test and verify pass.

### Task 2: Draft State Model

**Files:**
- Create: `src/main/java/com/studlgu/vkbot/service/handler/utils/EventDraft.java`
- Create: `src/main/java/com/studlgu/vkbot/service/handler/utils/EventDraftCache.java`
- Modify: `src/main/java/com/studlgu/vkbot/service/handler/utils/UserState.java`
- Test: covered by handler tests.

- [ ] Add states for title, date, time, description, and location.
- [ ] Add a draft cache with create/get/clear methods keyed by user id.

### Task 3: Command Start And Cancel

**Files:**
- Modify: `src/main/java/com/studlgu/vkbot/service/handler/command/impl/AddEventCommandHandler.java`
- Modify: `src/main/java/com/studlgu/vkbot/service/handler/command/impl/CancelCommandHandler.java`
- Test: `src/test/java/com/studlgu/vkbot/service/handler/command/impl/CancelCommandHandlerTest.java`

- [ ] Write failing test that cancel clears an event draft.
- [ ] Run the cancel handler test and verify failure.
- [ ] Inject `EventDraftCache` into add and cancel handlers.
- [ ] Make add command create a draft, set title state, and send `StandardKeyboard.createCancelKeyboard()`.
- [ ] Make cancel clear the draft as well as the state.
- [ ] Run the cancel handler test and verify pass.

### Task 4: Callback Routing

**Files:**
- Modify: `src/main/java/com/studlgu/vkbot/service/handler/callback/CallbackService.java`
- Test: `src/test/java/com/studlgu/vkbot/service/handler/callback/CallbackServiceTest.java`

- [ ] Write failing test that all add-event states route to `add_event_input`.
- [ ] Run the callback service test and verify failure.
- [ ] Route every add-event state to `add_event_input`.
- [ ] Run the callback service test and verify pass.

### Task 5: Dialog Handler

**Files:**
- Modify: `src/main/java/com/studlgu/vkbot/service/handler/callback/impl/AddEventInputHandler.java`
- Test: `src/test/java/com/studlgu/vkbot/service/handler/callback/impl/AddEventInputHandlerTest.java`

- [ ] Write failing tests for title, date, invalid date, skipped optional fields, invalid time, and final save.
- [ ] Run the handler test and verify failure.
- [ ] Implement step-specific input handling and messages.
- [ ] Save only after location step.
- [ ] Clear state and draft after successful save.
- [ ] Run the handler test and verify pass.

### Task 6: Optional Time Rendering

**Files:**
- Modify: `src/main/java/com/studlgu/vkbot/service/handler/command/impl/EventDayDetailCommandHandler.java`
- Test: covered by full test suite unless a focused test is added later.

- [ ] Only print the time line when `Event.time` is not null.
- [ ] Run `.\gradlew.bat test` and verify pass.
