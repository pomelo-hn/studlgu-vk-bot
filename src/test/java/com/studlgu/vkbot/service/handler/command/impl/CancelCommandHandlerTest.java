package com.studlgu.vkbot.service.handler.command.impl;

import com.studlgu.vkbot.model.CallbackMessage;
import com.studlgu.vkbot.model.CallbackObject;
import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.service.handler.command.CommandType;
import com.studlgu.vkbot.service.handler.utils.AppealAnswerDraftCache;
import com.studlgu.vkbot.service.handler.utils.EventDraftCache;
import com.studlgu.vkbot.service.handler.utils.RoleIdentifier;
import com.studlgu.vkbot.service.handler.utils.UserState;
import com.studlgu.vkbot.service.handler.utils.UserStateCache;
import com.studlgu.vkbot.service.handler.utils.VkActorFactory;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CancelCommandHandlerTest {

    @Test
    void handleClearsCurrentUserState() throws Exception {
        long userId = 123L;
        VkApiClient vkApiClient = mock(VkApiClient.class, RETURNS_DEEP_STUBS);
        VkActorFactory actorFactory = mock(VkActorFactory.class);
        RoleIdentifier roleIdentifier = mock(RoleIdentifier.class);
        UserStateCache userStateCache = new UserStateCache();
        UserActor userActor = new UserActor(userId, "token");
        userStateCache.setState(userId, UserState.AWAITING_PHOTO);
        when(actorFactory.create(userId)).thenReturn(userActor);
        when(roleIdentifier.hasEditorRights(vkApiClient, userActor)).thenReturn(true);
        CancelCommandHandler handler = new CancelCommandHandler(
                vkApiClient,
                actorFactory,
                roleIdentifier,
                userStateCache,
                new EventDraftCache(),
                new AppealAnswerDraftCache()
        );

        handler.handle(messageRequest(userId));

        assertThat(userStateCache.getState(userId)).isEmpty();
    }

    @Test
    void getTypeReturnsCancel() {
        CancelCommandHandler handler = new CancelCommandHandler(
                mock(VkApiClient.class, RETURNS_DEEP_STUBS),
                mock(VkActorFactory.class),
                mock(RoleIdentifier.class),
                new UserStateCache(),
                new EventDraftCache(),
                new AppealAnswerDraftCache()
        );

        assertThat(handler.getType()).isEqualTo(CommandType.CANCEL);
    }

    @Test
    void handleClearsCurrentEventDraft() throws Exception {
        long userId = 123L;
        VkApiClient vkApiClient = mock(VkApiClient.class, RETURNS_DEEP_STUBS);
        VkActorFactory actorFactory = mock(VkActorFactory.class);
        RoleIdentifier roleIdentifier = mock(RoleIdentifier.class);
        UserStateCache userStateCache = new UserStateCache();
        EventDraftCache eventDraftCache = new EventDraftCache();
        UserActor userActor = new UserActor(userId, "token");
        eventDraftCache.createDraft(userId).setTitle("Экзамен");
        when(actorFactory.create(userId)).thenReturn(userActor);
        when(roleIdentifier.hasEditorRights(vkApiClient, userActor)).thenReturn(true);
        CancelCommandHandler handler = new CancelCommandHandler(
                vkApiClient,
                actorFactory,
                roleIdentifier,
                userStateCache,
                eventDraftCache,
                new AppealAnswerDraftCache()
        );

        handler.handle(messageRequest(userId));

        assertThat(eventDraftCache.getDraft(userId)).isEmpty();
    }

    @Test
    void handleClearsCurrentAppealAnswerDraft() throws Exception {
        long userId = 123L;
        VkApiClient vkApiClient = mock(VkApiClient.class, RETURNS_DEEP_STUBS);
        VkActorFactory actorFactory = mock(VkActorFactory.class);
        RoleIdentifier roleIdentifier = mock(RoleIdentifier.class);
        UserStateCache userStateCache = new UserStateCache();
        EventDraftCache eventDraftCache = new EventDraftCache();
        AppealAnswerDraftCache appealAnswerDraftCache = new AppealAnswerDraftCache();
        UserActor userActor = new UserActor(userId, "token");
        appealAnswerDraftCache.setAppealId(userId, "appeal-1");
        when(actorFactory.create(userId)).thenReturn(userActor);
        when(roleIdentifier.hasEditorRights(vkApiClient, userActor)).thenReturn(true);
        CancelCommandHandler handler = new CancelCommandHandler(
                vkApiClient,
                actorFactory,
                roleIdentifier,
                userStateCache,
                eventDraftCache,
                appealAnswerDraftCache
        );

        handler.handle(messageRequest(userId));

        assertThat(appealAnswerDraftCache.getAppealId(userId)).isEmpty();
    }

    private CallbackRequest messageRequest(long userId) {
        CallbackMessage message = new CallbackMessage();
        message.setFromId(userId);

        CallbackObject object = new CallbackObject();
        object.setMessage(message);

        CallbackRequest request = new CallbackRequest();
        request.setType("message_new");
        request.setObject(object);
        return request;
    }
}
