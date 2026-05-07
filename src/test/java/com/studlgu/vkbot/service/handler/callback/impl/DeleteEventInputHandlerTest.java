package com.studlgu.vkbot.service.handler.callback.impl;

import com.studlgu.vkbot.model.CallbackMessage;
import com.studlgu.vkbot.model.CallbackObject;
import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.model.Payload;
import com.studlgu.vkbot.service.handler.utils.EventService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeleteEventInputHandlerTest {

    @Test
    void handleDeletesByEventIdFromPayload() {
        long userId = 123L;
        String eventId = "12345678-aaaa-bbbb-cccc-123456789abc";
        VkApiClient vkApiClient = mock(VkApiClient.class, RETURNS_DEEP_STUBS);
        VkActorFactory actorFactory = mock(VkActorFactory.class);
        RoleIdentifier roleIdentifier = mock(RoleIdentifier.class);
        EventService eventService = mock(EventService.class);
        UserStateCache userStateCache = new UserStateCache();
        UserActor userActor = new UserActor(userId, "token");
        userStateCache.setState(userId, UserState.AWAITING_DELETE_ID);
        when(actorFactory.create(userId)).thenReturn(userActor);
        when(eventService.deleteEventByPrefix(eventId)).thenReturn(true);
        DeleteEventInputHandler handler = new DeleteEventInputHandler(
                vkApiClient,
                actorFactory,
                roleIdentifier,
                eventService,
                userStateCache
        );

        handler.handle(messageRequest(userId, "ID: 12345678", eventId));

        verify(eventService).deleteEventByPrefix(eventId);
        assertThat(userStateCache.getState(userId)).isEmpty();
    }

    private CallbackRequest messageRequest(long userId, String text, String eventId) {
        Payload payload = new Payload();
        payload.setCommand("delete_event_by_id");
        payload.setEventId(eventId);

        CallbackMessage message = new CallbackMessage();
        message.setFromId(userId);
        message.setText(text);
        message.setMappedPayload(payload);

        CallbackObject object = new CallbackObject();
        object.setMessage(message);

        CallbackRequest request = new CallbackRequest();
        request.setType("message_new");
        request.setObject(object);
        return request;
    }
}
