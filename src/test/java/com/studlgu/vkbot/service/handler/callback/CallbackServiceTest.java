package com.studlgu.vkbot.service.handler.callback;

import com.studlgu.vkbot.model.CallbackMessage;
import com.studlgu.vkbot.model.CallbackObject;
import com.studlgu.vkbot.model.CallbackRequest;
import com.studlgu.vkbot.model.Payload;
import com.studlgu.vkbot.service.handler.utils.UserState;
import com.studlgu.vkbot.service.handler.utils.UserStateCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CallbackServiceTest {

    @Test
    void defineTypeRoutesCancelCommandBeforeAwaitingPhotoState() {
        UserStateCache userStateCache = new UserStateCache();
        CallbackService callbackService = new CallbackService(mock(ApplicationContext.class), userStateCache);
        long userId = 123L;
        userStateCache.setState(userId, UserState.AWAITING_PHOTO);

        CallbackRequest request = messageNewRequest(userId, "cancel");

        assertThat(callbackService.defineType(request)).isEqualTo("message_new");
    }

    @ParameterizedTest
    @EnumSource(value = UserState.class, names = {
            "AWAITING_EVENT_TITLE",
            "AWAITING_EVENT_DATE",
            "AWAITING_EVENT_TIME",
            "AWAITING_EVENT_DESCRIPTION",
            "AWAITING_EVENT_LOCATION"
    })
    void defineTypeRoutesAddEventDialogStatesToAddEventInput(UserState state) {
        UserStateCache userStateCache = new UserStateCache();
        CallbackService callbackService = new CallbackService(mock(ApplicationContext.class), userStateCache);
        long userId = 123L;
        userStateCache.setState(userId, state);

        CallbackRequest request = messageNewRequest(userId, null);

        assertThat(callbackService.defineType(request)).isEqualTo("add_event_input");
    }

    @ParameterizedTest
    @EnumSource(value = UserState.class, names = {
            "AWAITING_EVENT_TITLE",
            "AWAITING_EVENT_DATE",
            "AWAITING_EVENT_TIME",
            "AWAITING_EVENT_DESCRIPTION",
            "AWAITING_EVENT_LOCATION"
    })
    void defineTypeRoutesCancelCommandBeforeAddEventDialogStates(UserState state) {
        UserStateCache userStateCache = new UserStateCache();
        CallbackService callbackService = new CallbackService(mock(ApplicationContext.class), userStateCache);
        long userId = 123L;
        userStateCache.setState(userId, state);

        CallbackRequest request = messageNewRequest(userId, "cancel");

        assertThat(callbackService.defineType(request)).isEqualTo("message_new");
    }

    private CallbackRequest messageNewRequest(long userId, String command) {
        CallbackMessage message = new CallbackMessage();
        message.setFromId(userId);
        if (command != null) {
            Payload payload = new Payload();
            payload.setCommand(command);
            message.setMappedPayload(payload);
        }

        CallbackObject object = new CallbackObject();
        object.setMessage(message);

        CallbackRequest request = new CallbackRequest();
        request.setType("message_new");
        request.setObject(object);
        return request;
    }
}
