package com.studlgu.vkbot.service.handler.callback.impl;

import com.studlgu.vkbot.model.CallbackMessage;
import com.studlgu.vkbot.model.CallbackObject;
import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.utils.EventDraftCache;
import com.studlgu.vkbot.service.handler.utils.EventService;
import com.studlgu.vkbot.service.handler.utils.RoleIdentifier;
import com.studlgu.vkbot.service.handler.utils.UserState;
import com.studlgu.vkbot.service.handler.utils.UserStateCache;
import com.studlgu.vkbot.service.handler.utils.VkActorFactory;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddEventInputHandlerTest {

    @Test
    void titleStepStoresTitleAndAsksForDate() {
        TestContext ctx = new TestContext();
        ctx.draftCache.createDraft(ctx.userId);
        ctx.userStateCache.setState(ctx.userId, UserState.AWAITING_EVENT_TITLE);

        ctx.handler.handle(messageRequest(ctx.userId, "Экзамен"));

        assertThat(ctx.draftCache.getDraft(ctx.userId)).hasValueSatisfying(draft ->
                assertThat(draft.getTitle()).isEqualTo("Экзамен"));
        assertThat(ctx.userStateCache.getState(ctx.userId)).contains(UserState.AWAITING_EVENT_DATE);
    }

    @Test
    void dateStepStoresDateAndAsksForOptionalTime() {
        TestContext ctx = new TestContext();
        ctx.draftCache.createDraft(ctx.userId).setTitle("Экзамен");
        ctx.userStateCache.setState(ctx.userId, UserState.AWAITING_EVENT_DATE);

        ctx.handler.handle(messageRequest(ctx.userId, "2026-05-15"));

        assertThat(ctx.draftCache.getDraft(ctx.userId)).hasValueSatisfying(draft ->
                assertThat(draft.getDate()).isEqualTo(LocalDate.parse("2026-05-15")));
        assertThat(ctx.userStateCache.getState(ctx.userId)).contains(UserState.AWAITING_EVENT_TIME);
    }

    @Test
    void dateStepKeepsStateWhenDateIsInvalid() {
        TestContext ctx = new TestContext();
        ctx.draftCache.createDraft(ctx.userId).setTitle("Экзамен");
        ctx.userStateCache.setState(ctx.userId, UserState.AWAITING_EVENT_DATE);

        ctx.handler.handle(messageRequest(ctx.userId, "15.05.2026"));

        assertThat(ctx.draftCache.getDraft(ctx.userId)).hasValueSatisfying(draft ->
                assertThat(draft.getDate()).isNull());
        assertThat(ctx.userStateCache.getState(ctx.userId)).contains(UserState.AWAITING_EVENT_DATE);
    }

    @Test
    void optionalTimeCanBeSkipped() {
        TestContext ctx = new TestContext();
        ctx.draftCache.createDraft(ctx.userId)
                .setTitle("Экзамен")
                .setDate(LocalDate.parse("2026-05-15"));
        ctx.userStateCache.setState(ctx.userId, UserState.AWAITING_EVENT_TIME);

        ctx.handler.handle(messageRequest(ctx.userId, "Пропустить"));

        assertThat(ctx.draftCache.getDraft(ctx.userId)).hasValueSatisfying(draft ->
                assertThat(draft.getTime()).isNull());
        assertThat(ctx.userStateCache.getState(ctx.userId)).contains(UserState.AWAITING_EVENT_DESCRIPTION);
    }

    @Test
    void timeStepStoresValidTime() {
        TestContext ctx = new TestContext();
        ctx.draftCache.createDraft(ctx.userId)
                .setTitle("Экзамен")
                .setDate(LocalDate.parse("2026-05-15"));
        ctx.userStateCache.setState(ctx.userId, UserState.AWAITING_EVENT_TIME);

        ctx.handler.handle(messageRequest(ctx.userId, "09:30"));

        assertThat(ctx.draftCache.getDraft(ctx.userId)).hasValueSatisfying(draft ->
                assertThat(draft.getTime()).isEqualTo(LocalTime.parse("09:30")));
        assertThat(ctx.userStateCache.getState(ctx.userId)).contains(UserState.AWAITING_EVENT_DESCRIPTION);
    }

    @Test
    void timeStepKeepsStateWhenTimeIsInvalid() {
        TestContext ctx = new TestContext();
        ctx.draftCache.createDraft(ctx.userId)
                .setTitle("Экзамен")
                .setDate(LocalDate.parse("2026-05-15"));
        ctx.userStateCache.setState(ctx.userId, UserState.AWAITING_EVENT_TIME);

        ctx.handler.handle(messageRequest(ctx.userId, "утром"));

        assertThat(ctx.draftCache.getDraft(ctx.userId)).hasValueSatisfying(draft ->
                assertThat(draft.getTime()).isNull());
        assertThat(ctx.userStateCache.getState(ctx.userId)).contains(UserState.AWAITING_EVENT_TIME);
    }

    @Test
    void locationStepSavesEventAndClearsStateAndDraft() {
        TestContext ctx = new TestContext();
        ctx.draftCache.createDraft(ctx.userId)
                .setTitle("Экзамен")
                .setDate(LocalDate.parse("2026-05-15"))
                .setTime(LocalTime.parse("09:30"))
                .setDescription("Билеты 1-40");
        ctx.userStateCache.setState(ctx.userId, UserState.AWAITING_EVENT_LOCATION);

        ctx.handler.handle(messageRequest(ctx.userId, "Корпус А"));

        verify(ctx.eventService).addEvent(
                "Экзамен",
                LocalDate.parse("2026-05-15"),
                LocalTime.parse("09:30"),
                "Билеты 1-40",
                "Корпус А"
        );
        assertThat(ctx.userStateCache.getState(ctx.userId)).isEmpty();
        assertThat(ctx.draftCache.getDraft(ctx.userId)).isEmpty();
    }

    @Test
    void locationStepSavesSkippedOptionalFieldsAsNull() {
        TestContext ctx = new TestContext();
        ctx.draftCache.createDraft(ctx.userId)
                .setTitle("Экзамен")
                .setDate(LocalDate.parse("2026-05-15"));
        ctx.userStateCache.setState(ctx.userId, UserState.AWAITING_EVENT_LOCATION);

        ctx.handler.handle(messageRequest(ctx.userId, "Пропустить"));

        verify(ctx.eventService).addEvent("Экзамен", LocalDate.parse("2026-05-15"), null, null, null);
    }

    @Test
    void missingDraftDoesNotSaveEvent() {
        TestContext ctx = new TestContext();
        ctx.userStateCache.setState(ctx.userId, UserState.AWAITING_EVENT_DATE);

        ctx.handler.handle(messageRequest(ctx.userId, "2026-05-15"));

        verify(ctx.eventService, never()).addEvent(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
        assertThat(ctx.userStateCache.getState(ctx.userId)).isEmpty();
    }

    private static CallbackRequest messageRequest(long userId, String text) {
        CallbackMessage message = new CallbackMessage();
        message.setFromId(userId);
        message.setText(text);

        CallbackObject object = new CallbackObject();
        object.setMessage(message);

        CallbackRequest request = new CallbackRequest();
        request.setType("message_new");
        request.setObject(object);
        return request;
    }

    private static class TestContext {
        private final long userId = 123L;
        private final VkApiClient vkApiClient = mock(VkApiClient.class, RETURNS_DEEP_STUBS);
        private final VkActorFactory actorFactory = mock(VkActorFactory.class);
        private final RoleIdentifier roleIdentifier = mock(RoleIdentifier.class);
        private final EventService eventService = mock(EventService.class);
        private final UserStateCache userStateCache = new UserStateCache();
        private final EventDraftCache draftCache = new EventDraftCache();
        private final AddEventInputHandler handler;

        private TestContext() {
            UserActor userActor = new UserActor(userId, "token");
            when(actorFactory.create(userId)).thenReturn(userActor);
            handler = new AddEventInputHandler(
                    vkApiClient,
                    actorFactory,
                    roleIdentifier,
                    eventService,
                    userStateCache,
                    draftCache
            );
        }
    }
}
